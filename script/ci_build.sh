#!/bin/bash

# Function to compare JDK versions
version_compare() {
    local v1=$(echo "$1" | cut -d '.' -f2)
    local v2=$(echo "$2" | cut -d '.' -f2)

    if [[ "$v1" -eq "$v2" ]]; then
        return 1 # Less than
    fi
    return 0 # Greater than or equal
}

# Get the current JDK version
current_jdk=@0
echo "Current JDK version: $current_jdk"

# Define your submodules directory paths
readarray -t submodules < <(grep '<module>' "./pom.xml" | sed -E 's|.*<module>(.*)</module>.*|\1|')

# Loop through each submodule
for submodule in "${submodules[@]}"; do
    echo "Checking $submodule"
    # Extract the required JDK version from the submodule's pom.xml
    required_jdk=$(grep '<jdk.version>' "$submodule/pom.xml" | sed 's/.*<jdk.version>\(.*\)<\/jdk.version>.*/\1/')

    echo "Required JDK version for $submodule is $required_jdk"

    # Compare versions
    version_compare "$current_jdk" "$required_jdk"
    if [[ $? -eq 0 ]]; then
        echo "Building $submodule..."
        mvn clean install -pl "./$submodule"
    else
        echo "Skipping $submodule, JDK version requirement not met."
    fi
done
