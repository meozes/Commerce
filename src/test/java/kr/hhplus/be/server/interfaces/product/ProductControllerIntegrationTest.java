package kr.hhplus.be.server.interfaces.product;

import kr.hhplus.be.server.domain.order.entity.OrderItem;
import kr.hhplus.be.server.domain.order.entity.Order;
import kr.hhplus.be.server.domain.order.repository.OrderItemRepository;
import kr.hhplus.be.server.domain.order.repository.OrderRepository;
import kr.hhplus.be.server.domain.order.type.OrderStatusType;
import kr.hhplus.be.server.domain.order.usecase.OrderService;
import kr.hhplus.be.server.domain.product.entity.Product;
import kr.hhplus.be.server.domain.product.entity.Stock;
import kr.hhplus.be.server.domain.product.repository.ProductRepository;
import kr.hhplus.be.server.domain.product.repository.StockRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class ProductControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;


    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("hhplus")
            .withUsername("application")
            .withPassword("application");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @BeforeAll
    static void beforeAll() {
        mysql.start();
    }

    @AfterAll
    static void afterAll() {
        mysql.stop();
    }

    @BeforeEach
    void setUp() {
        // 테스트 데이터 세팅
        for (int i = 1; i <= 15; i++) {
            Product product = Product.builder()
                    .productName("쿠키" + i)
                    .price(1000 * i)
                    .build();
            productRepository.save(product);

            Stock stock = Stock.builder()
                    .product(product)
                    .originStock(100)
                    .remainingStock(100 - i)
                    .build();
            stockRepository.save(stock);
        }

        assertThat(orderService).isNotNull();

    }

    @Test
    @DisplayName("상품 단건 조회 API - 상품 단건 조회 성공한다.")
    void getProduct_success() throws Exception {
        // given
        Product savedProduct = productRepository.findAll().get(0);
        Stock savedStock = stockRepository.getStock(savedProduct.getId());

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/products/" + savedProduct.getId())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(savedProduct.getId()))
                .andExpect(jsonPath("$.data.productName").value(savedProduct.getProductName()))
                .andExpect(jsonPath("$.data.price").value(savedProduct.getPrice()))
                .andExpect(jsonPath("$.data.remainingStock").value(savedStock.getRemainingStock()));
    }

    @Test
    @DisplayName("상품 단건 조회 API - 존재하지 않는 상품 조회시 예외 발생한다.")
    void getProduct_notFound() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/products/999999")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 상품이 존재하지 않습니다."))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    @DisplayName("상품 단건 조회 API - 존재하지 않는 상품 조회시 예외 발생한다.")
    void getProduct_invalidId() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/products/-1")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("유효하지 않은 상품 ID 입니다."))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("상품 다건 조회 API - 상품 페이징하여 조회 성공한다.")
    void getProducts_success() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/products")
                        .param("page", "0")
                        .param("size", "5")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content", hasSize(5)))
                .andExpect(jsonPath("$.data.totalElements").value(15))
                .andExpect(jsonPath("$.data.totalPages").value(3))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.number").value(0));
    }

    @Test
    @DisplayName("인기 상품 조회 API - 3일간 인기 상품 5개 조회 성공한다.")
    void getTop5Products_success() throws Exception {
        // given
        Order order = Order.builder()
                .userId(1L)
                .originalAmount(100000)
                .finalAmount(100000)
                .discountAmount(0)
                .orderStatus(OrderStatusType.COMPLETED)
                .build();
        orderRepository.save(order);

        // 테스트용 주문 데이터 생성
        OrderItem orderItem1 = OrderItem.builder()
                .order(order)
                .productId(1L)
                .productName("쿠키1")
                .quantity(100)
                .productPrice(10000)
                .totalPrice(1000000)
                .build();

        OrderItem orderItem2 = OrderItem.builder()
                .order(order)
                .productId(2L)
                .productName("초콜릿")
                .quantity(80)
                .productPrice(15000)
                .totalPrice(1200000)
                .build();

        OrderItem orderItem3 = OrderItem.builder()
                .order(order)
                .productId(3L)
                .productName("빵")
                .quantity(60)
                .productPrice(20000)
                .totalPrice(1200000)
                .build();

        OrderItem orderItem4 = OrderItem.builder()
                .order(order)
                .productId(4L)
                .productName("우유")
                .quantity(40)
                .productPrice(25000)
                .totalPrice(1000000)
                .build();

        OrderItem orderItem5 = OrderItem.builder()
                .order(order)
                .productId(5L)
                .productName("케이크")
                .quantity(20)
                .productPrice(30000)
                .totalPrice(600000)
                .build();

        orderItemRepository.saveAll(Arrays.asList(
                orderItem1, orderItem2, orderItem3, orderItem4, orderItem5
        ));

        List<OrderItem> savedItems = orderItemRepository.findAll();
        assertThat(savedItems).hasSize(5);

        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/products/top")
                        .param("startDate", LocalDate.now().minusDays(3).toString())
                        .param("endDate", LocalDate.now().toString())
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products").isArray())
                .andExpect(jsonPath("$.data.products", hasSize(5)))
                .andExpect(jsonPath("$.data.products[0].rank").value(1))
                .andExpect(jsonPath("$.data.products[0].productId").value(1))
                .andExpect(jsonPath("$.data.products[0].productName").value("쿠키1"))
                .andExpect(jsonPath("$.data.products[0].totalQuantitySold").value(100))
                .andExpect(jsonPath("$.data.products[0].price").value(10000));
    }

    @Test
    @DisplayName("주문 데이터가 없을 경우 빈 리스트 반환")
    void getTop5Products_empty() throws Exception {
        // when
        ResultActions result = mockMvc.perform(
                MockMvcRequestBuilders
                        .get("/api/products/top")
                        .contentType(MediaType.APPLICATION_JSON)
        );

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.data.products").isArray())
                .andExpect(jsonPath("$.data.products", hasSize(0)));
    }

}
