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
    Cypress.env('ENTITY_TOPIC', name)
})

Given("two documents in the index", () => {

})

When("messages from {string} were sent to the topic {string}", (fixture, topic) => {
    cy.fixture(fixture).then((file) => {
        const body = Object.assign(file);
        cy.request({
            method: 'POST',
            url: 'http://localhost:32580/' + topic,
            body: body
        })
    })
})

Then("six messages are landing in the output topic", () => {
    cy.wait(2000)
})

Then("three messages are landing in the error topic", () => {

})

//cleanup
after(()=>{
    cy.request({
        method: 'DELETE',
        url: 'http://localhost:9200/' + Cypress.env('ENTITY_TOPIC')
    })
})