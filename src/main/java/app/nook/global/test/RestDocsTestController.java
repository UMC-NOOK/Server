package app.nook.global.test;

import app.nook.global.response.ApiResponse;
import app.nook.global.response.SuccessCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestDocsTestController {

    @GetMapping("/restDocsTest")
    public String restDocsTestAPI() {
        return "test!!";
    }

    @GetMapping("/apiResponseTest")
    public ApiResponse<String> test() { return ApiResponse.onSuccess("This is Test",SuccessCode.ACCEPTED); }
}