const { defineConfig } = require('cypress')
const allureWriter = require('@shelex/cypress-allure-plugin/writer');
const createBundler = require("@bahmutov/cypress-esbuild-preprocessor");
const {
    addCucumberPreprocessorPlugin,
} = require("@badeball/cypress-cucumber-preprocessor");
const {
    createEsbuildPlugin,
} = require("@badeball/cypress-cucumber-preprocessor/esbuild");

async function setupNodeEvents(on, config) {
    // This is required for the preprocessor to be able to generate JSON reports after each run, and more,
    await addCucumberPreprocessorPlugin(on, config);

    on(
        "file:preprocessor",
        createBundler({
            plugins: [createEsbuildPlugin(config)],
        })
    );
    allureWriter(on, config);
    // Make sure to return the config object as it might have been modified by the plugin.
    return config;
}

module.exports = defineConfig({
    video: true,
    e2e: {
        setupNodeEvents,
        baseUrl: 'http://localhost:9200',
        defaultCommandTimeout: 4000,
        pageLoadTimeout: 30000,
        viewportWidth: 1280,
        viewportHeight: 720,
        specPattern: "cypress/e2e/**/*.feature",
        excludeSpecPattern: '**/*.cy.js'
    },
    env: {
        allure: true,
        allureReuseAfterSpec: true,
        stepDefinitions: `cypress/e2e/**/*.js`
    }
})
