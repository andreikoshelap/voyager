package ee.voyagelog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VoyageProperties props;

    public WebConfig(VoyageProperties props) {
        this.props = props;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(props.corsAllowedOrigins().toArray(String[]::new))
                .allowedMethods("GET");
    }
}
