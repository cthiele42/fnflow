import { When, Then, Given } from "/usr/local/lib/node_modules/@badeball/cypress-cucumber-preprocessor";

Given("a searchtemplate with name {string} with:", (name, body) => {
    cy.request({
        method: 'POST',
        url: 'http://localhost:9200/_scripts/' + name,
        failOnStatusCode: true,
        body: JSON.parse(body)
    })
})

Given("an index {string} with mapping:", (name, body) => {
    cy.request({
        method: 'PUT',
        url: 'http://localhost:9200/' + name,
        failOnStatusCode: true,
        body: JSON.parse(body)
    })
})

Given("two documents in the index", () => {

})

When("ten input messages are provided", () => {

})

Then("six messages are landing in the output topic", () => {

})

Then("three messages are landing in the error topic", () => {

})