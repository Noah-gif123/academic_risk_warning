package dnui.springmvc1010.config;

import dnui.springmvc1010.interceptor.Interceptor1;
import dnui.springmvc1010.interceptor.Interceptor2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MyMVCConfig implements WebMvcConfigurer {
    @Autowired
    private Interceptor1 interceptor1;
    @Autowired
    private Interceptor2 interceptor2;

    //添加拦截器管理
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor1)
                .addPathPatterns("/**");
        registry.addInterceptor(interceptor2)
                .addPathPatterns("/**");
    }
}
