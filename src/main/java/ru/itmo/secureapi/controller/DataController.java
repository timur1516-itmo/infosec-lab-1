package ru.itmo.secureapi.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.secureapi.dto.CreateDataRequest;
import ru.itmo.secureapi.dto.DataResponse;
import ru.itmo.secureapi.service.DataService;

@RestController
@RequestMapping("/api/data")
public class DataController {

    private final DataService dataService;

    public DataController(DataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping
    public List<DataResponse> findAll() {
        return dataService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DataResponse create(@Valid @RequestBody CreateDataRequest request, Principal principal) {
        return dataService.create(request, principal.getName());
    }
}
