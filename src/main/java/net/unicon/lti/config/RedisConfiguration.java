package net.unicon.lti.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

@Configuration
@EnableRedisHttpSession
public class RedisConfiguration extends AbstractHttpSessionApplicationInitializer {

    @Bean
    public RedisConnectionFactory connectionFactory() {
        return new LettuceConnectionFactory();
    }
//
//    @Bean
//    RedisTemplate<String, Object> redisTemplate() {
//        final RedisTemplate<String, Object> template = new RedisTemplate<>();
//        template.setConnectionFactory(connectionFactory());
//        System.out.println("Default Serializer: " + template.getDefaultSerializer());
//        System.out.println("Key Serializer: " + template.getKeySerializer());
//        System.out.println("Hash Key Serializer: " + template.getHashKeySerializer());
//        System.out.println("Hash Value Serializer: " + template.getHashValueSerializer());
//        System.out.println("Value Serializer: " + template.getValueSerializer());
//        System.out.println("String Serializer: " + template.getStringSerializer());
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(new GenericToStringSerializer<>( Object.class ));
//        template.setValueSerializer(new GenericToStringSerializer<>( Object.class ));
//        return template;
//    }

    @Bean
    RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new Jackson2JsonRedisSerializer<Object>(Object.class);
    }

    @Bean
    public RedisTemplate<Object, Object> sessionRedisTemplate() {
        final RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setConnectionFactory(connectionFactory());
        return template;
    }

    private ClassLoader loader;

    @Bean
    public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
        return new Jackson2JsonRedisSerializer(objectMapper());
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
        return mapper;
    }
//
//    @Override
//    public void setBeanClassLoader(ClassLoader classLoader) {
//        this.loader = classLoader;
//    }


//    @Bean
//    public ServletContextInitializer servletContextInitializer() {
//        return new ServletContextInitializer() {
//
//            @Override
//            public void onStartup(ServletContext servletContext) throws ServletException {
//                System.out.println(servletContext.getSessionCookieConfig().getName());
//                servletContext.getSessionCookieConfig().setName("SESSION");
//                System.out.println(servletContext.getSessionCookieConfig().getName());
//            }
//        };
//
//    }

//    @Bean
//    public <S extends ExpiringSession> SessionRepositoryFilter<? extends ExpiringSession> springSessionRepositoryFilter(SessionRepository<S> sessionRepository, ServletContext servletContext) {
//        SessionRepositoryFilter<S> sessionRepositoryFilter = new SessionRepositoryFilter<S>(sessionRepository);
//        sessionRepositoryFilter.setServletContext(servletContext);
//        CookieHttpSessionStrategy httpSessionStrategy = new CookieHttpSessionStrategy();
//        httpSessionStrategy.setCookieName("MY_SESSION_NAME");
//        sessionRepositoryFilter.setHttpSessionStrategy(httpSessionStrategy);
//        return sessionRepositoryFilter;
//    }

//    @Bean
//    public CookieSerializer cookieSerializer() {
//        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
//        serializer.setCookieName("SESSION");
//        serializer.setCookiePath("/");
//        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$");
//        return serializer;
//    }

//    private ClassLoader loader;
//
//    @Bean
//    @Primary
//    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory cf) {
//        RedisTemplate<String, String> redisTemplate = new RedisTemplate<String, String>();
//        redisTemplate.setConnectionFactory(cf);
//        redisTemplate.setKeySerializer(redisTemplate.getStringSerializer());
//        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer(Object.class));
//        return redisTemplate;
//    }
//
//    @Bean
//    public CookieSerializer cookieSerializer() {
//        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
//        serializer.setCookieName("JSESSIONID"); // <1>
//        serializer.setCookiePath("/"); // <2>
//        serializer.setDomainNamePattern("^.+?\\.(\\w+\\.[a-z]+)$"); // <3>
//        return serializer;
//    }
//
//    @Bean
//    public HttpSessionStrategy httpSessionStrategy() {
//        return new HeaderHttpSessionStrategy();
//    }
//
//    @Bean
//    public LettuceConnectionFactory connectionFactory() {
//        return new LettuceConnectionFactory();
//    }
//
//    ObjectMapper objectMapper() {
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
//        return mapper;
//    }
//
//    public void setBeanClassLoader(ClassLoader classLoader) {
//        this.loader = classLoader;
//    }

}
