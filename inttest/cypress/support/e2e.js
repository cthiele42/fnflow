import '/usr/local/lib/node_modules/@shelex/cypress-allure-plugin';
import '/usr/local/lib/node_modules/cypress-plugin-api';

if (Cypress.spec.name.includes('test.duplicate.name')) {
    Cypress.Allure.reporter.getInterface().defineHistoryId((title) => {
        return `${Cypress.spec.relative}${title}`;
    });
}
