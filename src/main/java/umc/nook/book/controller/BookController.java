package umc.nook.book.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import umc.nook.book.service.BookService;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class BookController {

    private final BookService bookService;

}
