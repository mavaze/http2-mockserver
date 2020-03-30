# HTTP/2 Mock Server

## How to run
`java -jar bin/http2-mockserver-1.0.0.jar -f config.yaml -f config-1.yaml -f config-2.yaml`

## Folder Structure
- **bin**: The executable jar is located here. You can execute 'jar' from anywhere using absolute/relative path.
- **stubs**: Application looks for config files, those you pass with -f flag, in 'stubs' folder.
- **responses**: If you refer to file based responses in your config file, those files will be loaded from 'responses' folder.

## Config File
- You can pass multiple files as program arguments with '-f' flag.
- Alternatively, you may also pass multi-document yaml (one partitioned with --- line).

### ports
 - defines http and https ports that server will listen to. You can skip either of them if you don't want to open both ports.

### paths
1. **name**: (Optional) Useful for logging to verify which stub matches with your actual request.
    - If absent, we generate a default name from method and uri.

2. **request**: Defines http 'methods' (comma separated GET, POST, HEAD etc.) and the uri the request should match to.
    - A uri can be with path variables which application extracts for you to use it in response template.

3. **responses**: The mock response the application responds to the request on match. It represents a list, but only one is selected.
    - name: (Optional) Useful for logging to verify which response was selected for the request.
        - If absent, we generate a default name from status and selector.
    - **selector**: Decides which response from the list of responses for the matched request (method+uri).
        - A response with no selector or with value default will be evaluated to 'true'.
        - If multiple responses are eligible (including default ones), the first in the order will be selected.
    - **status**: Response HTTP status
    - **headers**: A map of response headers.
    - **body**:
        - **format**: Strategy to decide the source of response. Available options INLINE | FILE | REQUEST | EMPTY.
        - **file**: Response will be generated from this file, present in 'responses' folder. Result of _'format: FILE'_.
        - **inline**: You may define your multiline response in config itself. It's good choice for less response content. Result of _'format: INLINE'_.
        - You may want to return response same as request body, choose 'format: REQUEST' and for no response _'format: EMPTY'_.

## Templating
You may refer to some fields from request, in selector expression and also in response template.
### selector

1. `now` : LocalDateTime.now()
2. `global.hostname` : e.g. localhost

3. `request.path` : The request URI
4. `request.scheme` : http/https
5. `request.method` : GET/POST/HEAD etc.
6. `request.body` : Raw request body

    - If request header contains _'Content-Type: application/json'_, the request body is stored as an JSON object and you can refer to individual properties.
    e.g. if you request body is
        ```
        {
            "outer": {
                "inner": "test"
            }
        }
        ```

    then you may use `request.body.outer.inner`

7. All header, form, query and param represent a Map<String, List<String>>
    - `request.header['request-header-name']`
    - `request.form['form-name']` : When request header contains _'Content-Type: application/x-www-form-urlencoded'_
    - `request.query['request-query-param']`
    - `request.param['request-path-param]`

    The list items can be accessed by appending them with [index] e.g. `request.header['request-header-name'][2]`

### responses
While in selector, you can use the fields as it is while in response, they should be wrapped inside @{} like @{request.path}

Some examples:
* `@{request.param['vendorId']}`
* `@{request.query['region'][0]}`
* `@{request.header['Content-Type'][0]}`
* `@{now}`

* ```
  @if{request.header['Content-Type'].contains('application/json')}
    --- your text goes here ---
  @else{}
    --- your text goes here ---
  @else{}
    --- your text goes here ---
  @end{}
  ```

* ```
  @foreach{item : products}
    @{item.serialNumber}
  @end{}
  ```

* `@{now.minusDays(10).isBefore(java.time.LocalDateTime.now())}`
