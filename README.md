# Shop — prosta aplikacja warstwowa

Projekt startowy do zajęć: Java 21, Spring Boot, Hibernate (JPA), baza H2
oraz testy w Groovy i Spocku. Aplikacja obsługuje tylko zamówienia (`Order`).
Jedno zamówienie dotyczy jednego produktu opisanego nazwą.

## Warstwy

```text
żądanie HTTP + OrderRequest
          ↓
    OrderController       obsługa REST
          ↓
    OrderService          walidacja, logika i mapowanie na DTO
          ↓
    OrderRepository       zapis i odczyt encji Order przez JPA
          ↓
    Hibernate → H2
```

Pakiety znajdują się w `src/main/java/edu/arch/shop`:

| Pakiet | Odpowiedzialność |
| --- | --- |
| `order.controller` | `OrderController` — endpointy REST. |
| `order.service` | `OrderService` — walidacja i logika zamówień. |
| `order.repository` | `OrderRepository` — zapis i odczyt z bazy. |
| `order.entity` | `Order` — encja Hibernate. |
| `order.dto` | `OrderRequest` i `OrderResponse` — dane wejściowe i wyjściowe API. |
| `customer.entity` | Tylko encja `Customer` z polami `id` i `name`. |

Kontroler wywołuje serwis, a serwis repozytorium. API zwraca DTO.
Identyfikator nadaje baza, a wartość zamówienia oblicza serwis — klient ich nie podaje.
Kwoty są reprezentowane przez `BigDecimal`. Zależności przekazujemy przez konstruktory.
Metoda `validate` w serwisie sprawdza dane przed utworzeniem i aktualizacją zamówienia.

## Relacja z klientem

Pole `Order.customer` ma adnotacje `@ManyToOne` i `@JoinColumn(name = "customer_id")`.
Wiele zamówień może wskazywać tego samego klienta. Hibernate tworzy kolumnę
`orders.customer_id` z kluczem obcym do `customers.id`.

Relacja jest opcjonalna. Obecne API nadal przyjmuje tekstowe `customerName`;
powiązanie z encją `Customer` pokazuje test „wiele zamówień może należeć do jednego klienta”.
Test najpierw zapisuje klienta przez `EntityManager`, następnie przypisuje go do dwóch zamówień.

Zadanie dla studentów: dodać repozytorium, serwis, kontroler i DTO klienta,
a następnie zastąpić tekstowe `customerName` wyborem klienta przez `customerId`.

## Uruchomienie

Wymagany jest JDK 21. `JAVA_HOME` powinno wskazywać katalog JDK.
Projekt zawiera Gradle Wrapper, więc nie trzeba instalować Gradle osobno.

Windows / PowerShell:

```powershell
.\gradlew.bat bootRun
```

Linux / macOS:

```bash
./gradlew bootRun
```

API: `http://localhost:8080/orders`.

## API

| Metoda | Adres | Wynik |
| --- | --- | --- |
| `POST` | `/orders` | Utworzenie zamówienia: 201, DTO i nagłówek `Location`. |
| `GET` | `/orders` | Lista zamówień: 200 (początkowo `[]`). |
| `GET` | `/orders/{id}` | Jedno zamówienie: 200 lub 404. |
| `PUT` | `/orders/{id}` | Zastąpienie danych zamówienia: 200 lub 404. |
| `DELETE` | `/orders/{id}` | Usunięcie zamówienia: 204 lub 404. |

Przykładowe body dla `POST` i `PUT` (`Content-Type: application/json`):

```json
{
  "customerName": "Jan Kowalski",
  "productName": "Książka",
  "quantity": 2,
  "unitPrice": 19.99
}
```

Odpowiedź zawiera te same pola oraz `id` i `totalPrice` (w przykładzie `39.98`).
Obie nazwy są wymagane, nie mogą być puste ani dłuższe niż 100 znaków.
Liczba sztuk jest dodatnią liczbą całkowitą. Cena musi być dodatnia i mieć
maksymalnie 10 cyfr przed przecinkiem oraz 2 po przecinku.
Błędne dane zwracają HTTP 400. `PUT` wymaga wszystkich pól, tak samo jak `POST`.

Gotowe żądania do uruchomienia w IntelliJ IDEA znajdują się w [orders.http](orders.http).

## Baza H2

Konsola: `http://localhost:8080/h2-console`.

- JDBC URL: `jdbc:h2:mem:shop`
- User Name: `sa`
- Password: puste

Hibernate tworzy tabelę przy starcie. Baza działa w pamięci, więc ponowne
uruchomienie aplikacji usuwa dane. W konsoli można wykonać `SELECT * FROM orders;`.
Konfiguracja znajduje się w `src/main/resources/application.properties`.

## Testy

```powershell
.\gradlew.bat test
```

Na Linux / macOS: `./gradlew test`.

- `OrderServiceSpec` — testy jednostkowe z mockiem repozytorium; operacje na zamówieniach,
  walidacja, obliczanie kwot oraz brak zamówienia.
- `OrderRepositorySpec` — zapis i odczyt z H2, precyzja kwot, usuwanie i relacja z klientem.
- `OrderControllerSpec` — testy integracyjne REST przez MockMvc, z prawdziwym serwisem
  i bazą H2; CRUD, walidacja i odpowiedzi 404.

Testy znajdują się w `src/test/groovy/edu/arch/shop/order`, w podpakietach odpowiadających warstwom.
Bloki `given`, `when`, `then` opisują
przygotowanie, działanie i oczekiwany wynik. Tabele `where` uruchamiają ten sam
scenariusz dla różnych danych. Raport: `build/reports/tests/test/index.html`.

## Na początek zajęć

1. Uruchom aplikację i wykonaj żądania z `orders.http`.
2. Prześledź metodę `create`: kontroler → serwis → repozytorium → baza.
3. Porównaj encję z DTO i sprawdź dane w konsoli H2.
4. Uruchom testy i odczytaj scenariusze Spocka.
5. Jako ćwiczenie dodaj do zamówienia pole `comment` wraz z walidacją i testem.
