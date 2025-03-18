import { When, Then, Given } from "/usr/local/lib/node_modules/@badeball/cypress-cucumber-preprocessor";
import {recurse} from "/usr/local/lib/node_modules/cypress-recurse"

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
    Cypress.env('ENTITY_INDEX', name)
})

Given("documents from {string} were indexed to {string}", (fixture, index) => {
    cy.fixture(fixture).then((file) => {
        const docs = Object.assign(file);
        docs.docs.forEach(doc =>
            cy.request({
                method: 'PUT',
                url: 'http://localhost:9200/' + index + '/_doc/' + doc.id,
                body: doc.content
            })
        )
        cy.request({
            method: 'POST',
            url: 'http://localhost:9200/' + index + '/_refresh'
        })
    })
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

Then("a number of {int} messages are landing in the topic {string}", (expected, topic) => {
    recurse(
        () => cy.request({
            method: 'GET',
            url: 'http://localhost:32580/' + topic,
            failOnStatusCode: false
        }),
        (response) => response.body.messageCount === expected,
        {
            log: true,
            limit: 120,
            timeout: 60000,
            delay: 500
        }
    )
})

//cleanup
after(()=>{
    //delete entity index
    cy.request({
        method: 'DELETE',
        url: 'http://localhost:9200/' + Cypress.env('ENTITY_INDEX')
    })

    //delete all topics
    cy.request({
        method: 'DELETE',
        url: 'http://localhost:32580/fnFlowComposedFnBean-in-0'
    })
    cy.request({
        method: 'DELETE',
        url: 'http://localhost:32580/fnFlowComposedFnBean-out-0'
    })
    cy.request({
        method: 'DELETE',
        url: 'http://localhost:32580/fnFlowComposedFnBean-out-1'
    })
})