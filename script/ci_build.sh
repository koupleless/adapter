#!/bin/bash

# Function to compare JDK versions
version_compare() {
    local v1=$(echo "$1")
    local v2=$(echo "$2")

    if [[ "$v1" = "$v2" ]]; then
        return 1
    fi
    return 0
}

# Get the current JDK version
current_jdk=$1
mvnCommand=$2
echo "Current JDK version: $current_jdk"

# Define your submodules directory paths
readarray -t submodules < <(grep '<module>' "./pom.xml" | sed -E 's|.*<module>(.*)</module>.*|\1|')

# Loop through each submodule
for submodule in "${submodules[@]}"; do
    echo "Checking $submodule"
    # Extract the required JDK version from the submodule's pom.xml
    required_jdk=$(grep '<java.version>' "$submodule/pom.xml" | sed 's/.*<java.version>\(.*\)<\/java.version>.*/\1/')

    echo "Required JDK version for $submodule is $required_jdk"

    # Compare versions
    version_compare "$current_jdk" "$required_jdk"
    if [[ $? -eq 1 ]]; then
        echo "Building $submodule..."
        if [[ "$mvnCommand" = "install" ]]; then
            mvn clean install -pl "./$submodule"
        fi

        if [[ "$mvnCommand" = "snapshot" ]]; then
            mvn --batch-mode deploy -DskipTests -Psnapshot -pl "./$submodule"
        fi

        if [[ "$mvnCommand" = "release" ]]; then
            mvn --batch-mode deploy -DskipTests -Prelease -pl "./$submodule"
        fi
    else
        echo "Skipping $submodule, JDK version requirement not met."
    fi
done