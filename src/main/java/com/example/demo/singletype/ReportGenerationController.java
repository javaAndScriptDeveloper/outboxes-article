package com.example.demo.singletype;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportGenerationController {

    private final ReportGenerationService reportGenerationService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void generate(@RequestBody GenerateReportRequest request) {
        reportGenerationService.submit(new ReportGenerationCommand(request.from(), request.to()));
    }
}
