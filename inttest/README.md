# Cypress based Integration Tests
## Run Tests
```shell
docker build --tag 'cypresscucumber' cypress-cucumber
docker run -it --rm --network host -v .:/app -w /app -u $(id -u) cypresscucumber:latest
```
## Run Tests interactivly
```shell
docker build --tag 'cypresscucumber' cypress-cucumber
xhost + local:
docker run -it --rm --network host -e DISPLAY -v .:/app -w /app -u $(id -u) --entrypoint cypress cypresscucumber:latest open --project .
```
