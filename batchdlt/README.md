# Goal
A function should be able to pass a batch of messages to an external service.  
As an example, for each message a query to a database or a search engine has to be executed. Executing such queries in batch would improve the performance.  
Such a batching could be achieved with Flux.buffer() functionality. This enforces the usage of reactive functions and in this case, DLT of Spring Cloud Stream will not work, see: https://github.com/spring-cloud/spring-cloud-stream/pull/1730#issuecomment-1910536341.

This project is implementing DLT for reactive functions by utilizing two output streams, one being the regular stream out, the second one being the error stream bound to the DLT.
Utilizing functions with multiple outputs will break Spring Cloud Function composition, see: https://github.com/spring-cloud/spring-cloud-function/blob/9d57f8fcbdd115c42bdedbabb67a8b29bf3f3481/spring-cloud-function-context/src/main/java/org/springframework/cloud/function/context/catalog/SimpleFunctionRegistry.java#L660. Therefore, this project implements its own function composition for functions with regular out and error out.

This implementation has the following limitations:

- composition of DLT enabled functions cannot be combined with Spring Cloud Function composition
- implemented composition does not support argument conversion, the functions of the current implementation have fixed argument type for in and out: JsonNode.
- In contrast to Spring Cloud Stream DLT handling, the input of the failed function is put into the DLT, not the input from the composed function. In the fnflow implementation, the intermediate result from the function before the failed function is sent to DLT.
- error messages from batching functions might arrive out of order in relation to errors from non batched functions in the DLT.