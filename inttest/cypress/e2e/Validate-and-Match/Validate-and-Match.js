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

Given("a pipeline processing app with name {string} and with this configs:", (name, body) => {
    cy.request({
        method: 'POST',
        url: 'http://localhost:32581/pipelines/' + name,
        failOnStatusCode: true,
        body: JSON.parse(body)
    }).then(() => {
        return recurse(
            () => cy.request({
                method: 'GET',
                url: 'http://localhost:32581/pipelines/' + name + '/status',
                failOnStatusCode: false
            }),
            (response) => response.body.status === 'COMPLETED',
            {
                log: true,
                limit: 120,
                timeout: 60000,
                delay: 500
            }
        )
    }).then(() => {
          return cy.wait(2000);
    });

    Cypress.env('PIPELINE_NAME', name)
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
    return recurse(
        () => cy.request({
            method: 'GET',
            url: 'http://localhost:32580/' + topic,
            failOnStatusCode: false
        }),
        (response) => {
            if (response.status !== 200 && response.status !== 204) {
                return false;
            }
            return response.body.messageCount === expected;
        },
        {
            log: true,
            limit: 120,
            timeout: 60000,
            delay: 500
        }
    )
})

Then("in topic {string} all messages are having a key", (topic) => {
    cy.request({
        method: 'GET',
        url: 'http://localhost:32580/' + topic + '/0',
        failOnStatusCode: true
    }).then((response) => {
        expect(response.body.messages).to.be.an('array').that.is.not.empty;
        response.body.messages.forEach((m) => {
            expect(m.key, 'missing key in message').to.be.an('string').that.is.not.empty;
            expect(m.value, 'missing content in message').to.be.an('object').that.is.not.empty;
            expect(m.value.name, 'missing merged content in result entity').to.be.an('string').that.is.not.empty;
            expect(m.value.product.fullName, 'missing merged content in result entity').to.be.an('string').that.is.not.empty;
        })
    })
})

Then("a topic with name {string} and messageCount {int} exists", (topic, msgCount) => {
    cy.request({
        method: 'GET',
        url: 'http://localhost:32580/' + topic,
        failOnStatusCode: true
    }).then((response) => {
        expect(response.body.messageCount).to.be.equal(0);
    })
})

//cleanup
after(()=>{
    //delete pipeline
    cy.request({
        method: 'DELETE',
        url: 'http://localhost:32581/pipelines/' + Cypress.env('PIPELINE_NAME'),
    })

    //delete entity index
    cy.request({
        method: 'DELETE',
        url: 'http://localhost:9200/' + Cypress.env('ENTITY_INDEX')
    });

    //delete all topics
    deleteTopics(['input-topic', 'output-topic-wrong', 'output-topic', 'error-topic', 'source-topic'])
})

const deleteTopics = (topics) => {
    topics.forEach((name) => {
        cy.request({
            method: 'DELETE',
            url: 'http://localhost:32580/' + name
        })
    });
}