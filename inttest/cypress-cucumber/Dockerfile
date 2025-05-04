FROM cypress/included:14.1.0

RUN apt update && \
    apt install -y default-jre
RUN npm i -g  @badeball/cypress-cucumber-preprocessor \
              @bahmutov/cypress-esbuild-preprocessor \
              cypress-recurse \
              cypress-plugin-api \
              allure-commandline \
              @shelex/cypress-allure-plugin
