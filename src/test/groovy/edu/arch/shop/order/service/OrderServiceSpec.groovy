package edu.arch.shop.order.service

import edu.arch.shop.order.dto.OrderRequest
import edu.arch.shop.order.entity.Order
import edu.arch.shop.order.repository.OrderRepository
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import spock.lang.Specification
import spock.lang.Unroll

class OrderServiceSpec extends Specification {

    OrderRepository orderRepository = Mock()
    OrderService orderService = new OrderService(orderRepository)

    @Unroll
    def "tworzy zamówienie i oblicza wartość: #quantity * #unitPrice = #totalPrice"() {
        given:
        def request = new OrderRequest("Jan Kowalski", "Książka", quantity, unitPrice)

        when:
        def response = orderService.create(request)

        then:
        1 * orderRepository.save(_ as Order) >> { Order order ->
            assert order.customerName == "Jan Kowalski"
            assert order.productName == "Książka"
            assert order.quantity == quantity
            assert order.unitPrice == unitPrice
            order
        }
        response.customerName() == "Jan Kowalski"
        response.productName() == "Książka"
        response.quantity() == quantity
        response.unitPrice() == unitPrice
        response.totalPrice() == totalPrice

        where:
        quantity | unitPrice | totalPrice
        2        | 19.99G    | 39.98G
        3        | 0.10G     | 0.30G
        1        | 25.00G    | 25.00G
    }

    def "zwraca listę zamówień jako DTO"() {
        given:
        orderRepository.findAll() >> [
                new Order("Jan", "Książka", 2, 19.99G),
                new Order("Anna", "Notes", 3, 5.00G)
        ]

        when:
        def orders = orderService.findAll()

        then:
        orders.size() == 2
        orders[0].customerName() == "Jan"
        orders[0].totalPrice() == 39.98G
        orders[1].customerName() == "Anna"
        orders[1].totalPrice() == 15.00G
    }

    def "zwraca pustą listę gdy nie ma zamówień"() {
        given:
        orderRepository.findAll() >> []

        expect:
        orderService.findAll().isEmpty()
    }

    def "pobiera zamówienie po identyfikatorze"() {
        given:
        orderRepository.findById(1L) >> Optional.of(new Order("Jan", "Książka", 2, 19.99G))

        when:
        def response = orderService.findById(1L)

        then:
        response.customerName() == "Jan"
        response.productName() == "Książka"
        response.totalPrice() == 39.98G
    }

    def "aktualizuje dane i ponownie oblicza wartość zamówienia"() {
        given:
        def order = new Order("Jan", "Książka", 2, 19.99G)
        def request = new OrderRequest("Anna", "Notes", 3, 5.50G)
        orderRepository.findById(1L) >> Optional.of(order)

        when:
        def response = orderService.update(1L, request)

        then:
        1 * orderRepository.save(order) >> order
        order.customerName == "Anna"
        order.productName == "Notes"
        order.quantity == 3
        order.unitPrice == 5.50G
        response.customerName() == "Anna"
        response.productName() == "Notes"
        response.quantity() == 3
        response.unitPrice() == 5.50G
        response.totalPrice() == 16.50G
    }

    def "usuwa istniejące zamówienie"() {
        given:
        def order = new Order("Jan", "Książka", 2, 19.99G)
        orderRepository.findById(1L) >> Optional.of(order)

        when:
        orderService.delete(1L)

        then:
        1 * orderRepository.delete(order)
    }

    def "nie zapisuje zamówienia z niedodatnią liczbą sztuk"() {
        given:
        def request = new OrderRequest("Jan", "Notes", 0, 5.00G)

        when:
        orderService.create(request)

        then:
        def exception = thrown(ResponseStatusException)
        exception.statusCode == HttpStatus.BAD_REQUEST
        0 * orderRepository._
    }

    def "sprawdza cenę przed aktualizacją zamówienia"() {
        given:
        def request = new OrderRequest("Anna", "Notes", 2, -1.00G)

        when:
        orderService.update(1L, request)

        then:
        def exception = thrown(ResponseStatusException)
        exception.statusCode == HttpStatus.BAD_REQUEST
        0 * orderRepository._
    }

    def "zgłasza brak zamówienia przy pobieraniu"() {
        given:
        orderRepository.findById(99L) >> Optional.empty()

        when:
        orderService.findById(99L)

        then:
        def exception = thrown(ResponseStatusException)
        exception.statusCode == HttpStatus.NOT_FOUND
    }

    def "nie zapisuje zmian gdy aktualizowane zamówienie nie istnieje"() {
        given:
        orderRepository.findById(99L) >> Optional.empty()

        when:
        orderService.update(99L, new OrderRequest("Anna", "Notes", 1, 5.00G))

        then:
        thrown(ResponseStatusException)
        0 * orderRepository.save(_)
    }

    def "zgłasza brak zamówienia przy usuwaniu"() {
        given:
        orderRepository.findById(99L) >> Optional.empty()

        when:
        orderService.delete(99L)

        then:
        thrown(ResponseStatusException)
        0 * orderRepository.delete(_)
    }
}
