package cn.edu.neu.springbootdemo.core.common;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import cn.edu.neu.springbootdemo.core.Constants;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

@Configuration
public class MyInterceptorConfig implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// TODO Auto-generated method stub
		WebMvcConfigurer.super.addInterceptors(registry);
		// 注册CrossOriginInterceptor拦截器
		registry.addInterceptor(new CrossOriginInterceptor());
		// 注册LoginInterceptor拦截器
		InterceptorRegistration registration = registry.addInterceptor(new LoginInterceptor());
		registration.addPathPatterns("/**"); // 所有路径都被拦截
		registration.excludePathPatterns( // 添加不拦截路径
				"/user/login", // 登录
				"/user/logout", // 退出登录
				"/user/addUser", // 注册
				"/user/checkUsername", // 检查用户名
				"/error", // 报错
				"/**/*.html", // html静态资源
				"/**/*.js", // js静态资源
				"/**/*.css", // css静态资源
				"/upload/**" // 照片墙上传的图片
		);	
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		WebMvcConfigurer.super.addResourceHandlers(registry);
		// 照片墙上传的图片保存在磁盘目录，映射成 /upload/** 供前端直接访问
		registry.addResourceHandler("/upload/**").addResourceLocations("file:" + Constants.UPLOAD_DIR);
	}

//	@Bean
//	public MappingJackson2HttpMessageConverter customJackson2HttpMessageConverter() {
//		MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
//		ObjectMapper objectMapper = new ObjectMapper();
//		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
//		objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
//		objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
//		objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
//		objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
//		jsonConverter.setObjectMapper(objectMapper);
//		return jsonConverter;
//	}
//
//	@Override
//	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//		// TODO Auto-generated method stub	
//		converters.add(customJackson2HttpMessageConverter());
//		WebMvcConfigurer.super.configureMessageConverters(converters);
//	}

//	@Override
//	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
//	    //调用父类的配置
//	    super.configureMessageConverters(converters);
//	    //创建fastJson消息转换器
//	    FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
//	    //创建配置类，添加fastJson的配置信息，比如：是否要格式化返回的json数据
//	    FastJsonConfig fastJsonConfig = new FastJsonConfig();
//	    //修改配置返回内容的过滤
//	    fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat,
//	                SerializerFeature.WriteMapNullValue,
//	                SerializerFeature.WriteNullStringAsEmpty,
//	                SerializerFeature.DisableCircularReferenceDetect,
//	                SerializerFeature.WriteNullListAsEmpty,
//	                SerializerFeature.WriteDateUseDateFormat);
//	    //处理中文乱码问题
//	    List<MediaType> fastMediaTypes = new ArrayList<>();
//	    fastMediaTypes.add(MediaType.APPLICATION_JSON_UTF8);
//	    //在convert中添加配置信息.
//	    fastJsonHttpMessageConverter.setSupportedMediaTypes(fastMediaTypes);
//	    fastConverter.setFastJsonConfig(fastJsonConfig);
//	    //将fastjson添加到视图消息转换器列表内
//	    converters.add(fastConverter);
//	}

}
