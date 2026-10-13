package dnui.springmvc1010;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class SpringMvc1010Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringMvc1010Application.class, args);
    }

}