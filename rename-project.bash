#!/usr/bin/env bash

set -e

if ! echo "$BASH_VERSION" | grep -E "^[45]" &>/dev/null; then
  echo "Found bash version: $BASH_VERSION"
  echo "Ensure you are using bash version 4 or 5"
  exit 1
fi

if [[ $# -ge 1 ]]; then
  PROJECT_INPUT=$1
  SLACK_RELEASES_CHANNEL=$2
  SECURITY_ALERTS_SLACK_CHANNEL_ID=$3
  NON_PROD_ALERTS_SEVERITY_LABEL=$4
  PROD_ALERTS_SEVERITY_LABEL=$5
  PRODUCT_ID=$6
else
  read -rp "New project name e.g. prison-visits >" PROJECT_INPUT
  read -rp "Slack channel for release notifications >" SLACK_RELEASES_CHANNEL
  read -rp "Slack channel for pipeline security notifications >" SECURITY_ALERTS_SLACK_CHANNEL_ID
  echo "For configurating alert severity labels, please first see https://user-guide.cloud-platform.service.justice.gov.uk/documentation/monitoring-an-app/how-to-create-alarms.html#creating-your-own-custom-alerts"
  read -rp "Non-prod kubernetes alerts. The severity label used by prometheus to route alert notifications to slack >" NON_PROD_ALERTS_SEVERITY_LABEL
  read -rp "Production kubernetes alerts. The severity label used by prometheus to route alert notifications to slack >" PROD_ALERTS_SEVERITY_LABEL
  echo "Refer to the developer portal at https://developer-portal.hmpps.service.justice.gov.uk/products to find your product id."
  read -rp "Provide an ID for the product this app/component belongs too >" PRODUCT_ID
fi

PROJECT_NAME_LOWER=${PROJECT_INPUT,,}                 # lowercase
PROJECT_NAME_HYPHENS=${PROJECT_NAME_LOWER// /-}       # spaces to hyphens

PROJECT_NAME=${PROJECT_NAME_HYPHENS//[^a-z0-9-]/}     # remove all other characters
PROJECT_NAME_WITHOUT_HMPPS=${PROJECT_NAME/hmpps-/}    # remove hmpps prefix
PACKAGE_NAME=${PROJECT_NAME_WITHOUT_HMPPS//-/}        # remove hyphen

read -ra PROJECT_NAME_ARRAY <<<"${PROJECT_NAME//-/ }" # convert to array
PROJECT_DESCRIPTION_HMPPS_LOWER=${PROJECT_NAME_ARRAY[*]^} # convert array back to string thus capitalising first character
PROJECT_DESCRIPTION=${PROJECT_DESCRIPTION_HMPPS_LOWER/Hmpps/HMPPS} # ensure that HMPPS is capitalised
CLASS_NAME=${PROJECT_DESCRIPTION_HMPPS_LOWER// /}     # then remove spaces

echo "Found:      Project of $PROJECT_DESCRIPTION"
echo "       Project name of $PROJECT_NAME"
echo "       Package name of $PACKAGE_NAME"
echo "         Class name of $CLASS_NAME"

echo "Performing search and replace"

# exclude files that get in the way and don't make any difference
EXCLUDES="( -path ./build -o -path ./out -o -path ./.git -o -path ./.gradle -o -path ./gradle -o -path ./.idea -o -path ./rename-project.bash )"
# shellcheck disable=SC2086
find . $EXCLUDES -prune -o -type f -exec sed -i \
  -e "s/hmpps-template-kotlin/$PROJECT_NAME/g" \
  -e "s/template-kotlin/$PROJECT_NAME_WITHOUT_HMPPS/g" \
  -e "s/HMPPS Template Kotlin/$PROJECT_DESCRIPTION/g" \
  -e "s/HmppsTemplateKotlin/$CLASS_NAME/g" \
  -e "s/templatepackagename/$PACKAGE_NAME/g" {} \;

echo "Performing directory renames"

# move package directory to new name
BASE="kotlin/uk/gov/justice/digital/hmpps"
mv "src/test/${BASE}/templatepackagename" "src/test/$BASE/$PACKAGE_NAME"
mv "src/main/${BASE}/templatepackagename" "src/main/$BASE/$PACKAGE_NAME"

# and move helm stuff to new name
mv "helm_deploy/hmpps-template-kotlin" "helm_deploy/$PROJECT_NAME"

# Update helm values.yaml with product ID.
sed -i -z -E \
  -e "s/UNASSIGNED/$PRODUCT_ID/" \
  helm_deploy/$PROJECT_NAME/values.yaml

# Update helm values files with correct slack channels.
sed -i -z -E \
  -e "s/NON_PROD_ALERTS_SEVERITY_LABEL/$NON_PROD_ALERTS_SEVERITY_LABEL/" \
  helm_deploy/values-dev.yaml helm_deploy/values-preprod.yaml

sed -i -z -E \
  -e "s/PROD_ALERTS_SEVERITY_LABEL/$PROD_ALERTS_SEVERITY_LABEL/" helm_deploy/values-prod.yaml

# rename kotlin files
mv "src/main/$BASE/$PACKAGE_NAME/HmppsTemplateKotlin.kt" "src/main/$BASE/$PACKAGE_NAME/$CLASS_NAME.kt"
mv "src/main/$BASE/$PACKAGE_NAME/config/HmppsTemplateKotlinExceptionHandler.kt" "src/main/$BASE/$PACKAGE_NAME/config/${CLASS_NAME}ExceptionHandler.kt"

# change cron job to be random time otherwise we hit rate limiting with veracode
RANDOM_HOUR=$((RANDOM % (9 - 3 + 1) + 3))
RANDOM_MINUTE=$(($RANDOM%60))
RANDOM_MINUTE2=$(($RANDOM%60))
sed -i -z -E \
  -e "s/SLACK_RELEASES_CHANNEL/$SLACK_RELEASES_CHANNEL/" \
  .circleci/config.yml

sed -i -z -E \
  -e "s/on:\n  workflow_dispatch:\n  schedule:\n    - cron: \"19 6/on:\n  workflow_dispatch:\n  schedule:\n    - cron: \"$RANDOM_MINUTE $RANDOM_HOUR/" \
  -e "s/on:\n  workflow_dispatch:\n  schedule:\n    - cron: \"34 6/on:\n  workflow_dispatch:\n  schedule:\n    - cron: \"$RANDOM_MINUTE2 $RANDOM_HOUR/" \
  -e "s/C05J915DX0Q/$SECURITY_ALERTS_SLACK_CHANNEL_ID/" \
  .github/workflows/*

# lastly remove ourselves
rm rename-project.bash

echo "Completed."
echo "Please now review changes and generate a banner for src/main/resources/banner.txt."
echo "There are TODOs in the codebase to guide you through the changes."
