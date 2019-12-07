# Usage

Add to your POM

```xml
<dependency>
    <groupId>com.github.miamish</groupId>
    <artifactId>iam-rds-connector</artifactId>
    <version>1.0.0</version>
</dependency>
```

You can initialize the data source using spring configuration:

```
datasource:
  url: jdbc:mysql://my-cool-rds.123456789012.us-east-2.rds.amazonaws.com:3306/my-cool-schema
  username: my_cool_username
  type: com.github.miamish.connectors.rds.iam.IamAuthenticationDataSource
```

Alternatively, you can initialize in your code:

```
@Bean 
public DataSource dataSource() { 
    PoolConfiguration props = new PoolProperties(); 
    props.setUrl("jdbc:mysql://dbname.abc123xyz.us-east-1.rds.amazonaws.com/dbschema"); 
    props.setUsername("iam_dbuser_app"); 
    props.setDriverClassName("com.mysql.jdbc.Driver"); 
    return new IamAuthenticationDataSource(props); 
}
```

# Release

```
mvn clean deploy
```