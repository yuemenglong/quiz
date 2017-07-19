package yy.rdpac;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by <yuemenglong@126.com> on 2017/7/19.
 */
@Controller
@RequestMapping("/")
@SpringBootApplication(scanBasePackages = "yy.rdpac")
public class App {

    public static void main(String args[]) {
        SpringApplication.run(App.class);
    }

    @ResponseBody
    @RequestMapping("/")
    public String index() {
        return "hello";
    }
}
