package edu.arch.shop.order.repository

import edu.arch.shop.customer.entity.Customer
import edu.arch.shop.order.entity.Order
import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import spock.lang.Specification

@DataJpaTest
class OrderRepositorySpec extends Specification {

    @Autowired
    OrderRepository orderRepository

    @Autowired
    EntityManager entityManager

    def "zapisuje zamówienie w H2 i nadaje identyfikator"() {
        when:
        def saved = orderRepository.saveAndFlush(new Order("Jan Kowalski", "Książka", 2, 19.99G))
        entityManager.clear()
        def loaded = orderRepository.findById(saved.id).orElseThrow()

        then:
        loaded.id != null
        loaded.customerName == "Jan Kowalski"
        loaded.productName == "Książka"
        loaded.quantity == 2
        loaded.unitPrice == 19.99G
    }

    def "przechowuje maksymalną dozwoloną cenę bez utraty precyzji"() {
        when:
        def saved = orderRepository.saveAndFlush(new Order("Anna", "Produkt", 1, 9999999999.99G))
        entityManager.clear()

        then:
        orderRepository.findById(saved.id).orElseThrow().unitPrice == 9999999999.99G
    }

    def "usuwa zamówienie z bazy"() {
        given:
        def order = orderRepository.saveAndFlush(new Order("Jan", "Notes", 1, 5.00G))

        when:
        orderRepository.delete(order)
        orderRepository.flush()
        entityManager.clear()

        then:
        orderRepository.findById(order.id).isEmpty()
    }

    def "wiele zamówień może należeć do jednego klienta"() {
        given:
        def customer = new Customer(name: "Jan Kowalski")
        entityManager.persist(customer)

        def firstOrder = new Order("Jan Kowalski", "Książka", 2, 19.99G)
        firstOrder.setCustomer(customer)
        def secondOrder = new Order("Jan Kowalski", "Notes", 1, 5.00G)
        secondOrder.setCustomer(customer)

        when:
        orderRepository.saveAndFlush(firstOrder)
        orderRepository.saveAndFlush(secondOrder)
        entityManager.clear()

        then:
        def loadedFirst = orderRepository.findById(firstOrder.id).orElseThrow()
        def loadedSecond = orderRepository.findById(secondOrder.id).orElseThrow()
        loadedFirst.customer.id == customer.id
        loadedSecond.customer.id == customer.id
        loadedFirst.customer.name == "Jan Kowalski"
        loadedSecond.customer.name == "Jan Kowalski"
    }
}
