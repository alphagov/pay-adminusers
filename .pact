pact_dir=src/test/resources/pacts
pact_cmd="mvn clean package -Dtest=ContractTestSuite -DpactSource=local"
pact_consumers=( selfservice )
