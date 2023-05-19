#!/bin/bash -e

# Arguments:
# $1 -  FORK_VERSION         - Version of fork to install

FORK_VERSION="${1}"

if [ $# -eq 0 ]
  then
    echo "Please specify version as first parameter"
    exit 1
fi
echo "============================================================================================================"
echo "    Setting release version for dependencies"
echo "============================================================================================================"
mvn versions:set -DremoveSnapshot

echo "============================================================================================================"
echo "    Installing org.eclipse.persistence:org.eclipse.persistence.core:${FORK_VERSION} to Maven Local"
echo "============================================================================================================"
cd foundation/org.eclipse.persistence.core
mvn versions:set -DnewVersion="${FORK_VERSION}"
mvn install -f pom.xml

cd ../..

echo "============================================================================================================"
echo "    Installing org.eclipse.persistence:org.eclipse.persistence.jpa:${FORK_VERSION} to Maven Local"
echo "============================================================================================================"
cd jpa/org.eclipse.persistence.jpa
mvn versions:set -DnewVersion="${FORK_VERSION}"
mvn install -f pom.xml

cd ../..

echo "============================================================================================================"
echo "    Return to the snapshot version"
echo "============================================================================================================"
mvn versions:revert