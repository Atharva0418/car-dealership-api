# Test Evidence

## Backend Tests

![Backend test results 1](docs/images/backend-test1.png)

![Backend test results 2](docs/images/backend-test2.png)

- These test reports are generated on every test run locally by: 

```powershell
.\gradlew  test
```

- Find these reports in the following directory after executing the test command:

```
root-project-directory\build\reports\tests\test\index.html
```


## Frontend Tests

![Frontend test results](docs/images/frontend-test1.png)

- These test reports are generated on every test run locally by: 

```powershell
npx vitest test --coverage
```