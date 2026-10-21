package edu.arch.shop.order.controller

import edu.arch.shop.order.entity.Order
import edu.arch.shop.order.repository.OrderRepository
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    OrderRepository orderRepository

    def setup() {
        orderRepository.deleteAll()
    }

    def "obsługuje utworzenie, odczyt, aktualizację i usunięcie zamówienia"() {
        when: "klient tworzy zamówienie"
        def result = mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonOutput.toJson(validOrder())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath('$.customerName').value("Jan Kowalski"))
                .andExpect(jsonPath('$.productName').value("Książka"))
                .andExpect(jsonPath('$.quantity').value(2))
                .andExpect(jsonPath('$.unitPrice').value(19.99d))
                .andExpect(jsonPath('$.totalPrice').value(39.98d))
                .andReturn()
        def body = new JsonSlurper().parseText(result.response.contentAsString)
        Long id = body.id as Long

        then:
        id > 0
        result.response.getHeader("Location") == "/orders/$id"
        orderRepository.findById(id).orElseThrow().quantity == 2

        when: "klient pobiera zamówienie i listę"
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(id.intValue()))
                .andExpect(jsonPath('$.totalPrice').value(39.98d))
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.length()').value(1))
                .andExpect(jsonPath('$[0].id').value(id.intValue()))

        then:
        noExceptionThrown()

        when: "klient zmienia dane zamówienia"
        def updated = [customerName: "Anna Nowak", productName: "Notes", quantity: 3, unitPrice: 5.50]
        mockMvc.perform(put("/orders/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonOutput.toJson(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath('$.id').value(id.intValue()))
                .andExpect(jsonPath('$.customerName').value("Anna Nowak"))
                .andExpect(jsonPath('$.productName').value("Notes"))
                .andExpect(jsonPath('$.quantity').value(3))
                .andExpect(jsonPath('$.unitPrice').value(5.50d))
                .andExpect(jsonPath('$.totalPrice').value(16.50d))

        then: "zmiany są zapisane w bazie"
        def stored = orderRepository.findById(id).orElseThrow()
        stored.customerName == "Anna Nowak"
        stored.productName == "Notes"
        stored.quantity == 3
        stored.unitPrice == 5.50G
        orderRepository.count() == 1

        when: "klient usuwa zamówienie"
        mockMvc.perform(delete("/orders/{id}", id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
        mockMvc.perform(get("/orders/{id}", id))
                .andExpect(status().isNotFound())

        then:
        orderRepository.findById(id).isEmpty()
    }

    def "zwraca pustą listę gdy baza jest pusta"() {
        expect:
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"))
    }

    @Unroll
    def "zwraca 404 dla nieistniejącego zamówienia: #method"() {
        expect:
        mockMvc.perform(request(HttpMethod.valueOf(method), "/orders/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonOutput.toJson(validOrder())))
                .andExpect(status().isNotFound())

        where:
        method << ["GET", "PUT", "DELETE"]
    }

    @Unroll
    def "odrzuca błędne dane przy tworzeniu i aktualizacji: #field = #value"() {
        given:
        def existing = orderRepository.save(new Order("Jan Kowalski", "Książka", 2, 19.99G))
        def invalid = validOrder()
        invalid[field] = value
        def json = JsonOutput.toJson(invalid)

        expect:
        mockMvc.perform(post("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
        mockMvc.perform(put("/orders/{id}", existing.id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
        orderRepository.count() == 1
        def stored = orderRepository.findById(existing.id).orElseThrow()
        stored.customerName == "Jan Kowalski"
        stored.productName == "Książka"
        stored.quantity == 2
        stored.unitPrice == 19.99G

        where:
        field          | value
        "customerName" | " "
        "customerName" | null
        "customerName" | "a" * 101
        "productName"  | ""
        "productName"  | null
        "productName"  | "a" * 101
        "quantity"     | 0
        "quantity"     | -1
        "quantity"     | null
        "unitPrice"    | 0
        "unitPrice"    | -1.00
        "unitPrice"    | null
        "unitPrice"    | 1.001
        "unitPrice"    | 10000000000.00
    }

    private static Map validOrder() {
        [customerName: "Jan Kowalski", productName: "Książka", quantity: 2, unitPrice: 19.99]
    }
}
