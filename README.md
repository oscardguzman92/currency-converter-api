# Currency Converter API
Esta es una aplicación Spring Boot que proporciona un servicio de conversión de divisas, siguiendo principios de 
Clean Architecture y empleando patrones de diseño para garantizar su mantenibilidad, testabilidad y resiliencia en 
un entorno de negocio real. 

## Tabla de Contenidos
1.  [Descripción General](#1-descripción-general)
2.  [Características](#2-características)
3.  [Decisiones de Diseño y Arquitectura](#3-decisiones-de-diseño-y-arquitectura)
4.  [Tecnologías Utilizadas](#4-tecnologías-utilizadas)
5.  [Requisitos](#5-requisitos)
6.  [Cómo Compilar y Ejecutar](#6-cómo-compilar-y-ejecutar)
    * [Localmente](#localmente)
    * [Con Docker](#con-docker)
7.  [Endpoints de la API](#7-endpoints-de-la-api)
8.  [Pruebas](#8-pruebas)
9.  [Próximos Pasos / Mejoras Potenciales](#9-próximos-pasos--mejoras-potenciales)
10. [Contacto](#10-contacto)

---

### 1. Descripción General
 Este proyecto tiene como propósito realizar una API de conversión de monedas.
 Su funcionalidad principal radica en la consulta y procesamiento de tasas de 
 cambio obtenidas de una API externa (Exchange Rates API), que utiliza el Euro  (EUR),
 como divisa base. El objetivo es proporcionar una demostración práctica de como establecer 
 una base sólida para un proyecto de microservicio, enfatizando en la creación de una aplicación
 escalable,mantenible, adaptable y resiliente frente a cambios y errores, lo que la hace ideal
 para replicar en un dominio de negocio real. 

### 2. Características
* Conversión de divisas (Origen -> EUR -> Destino):
  La lógica de conversión se gestiona explícitamente en dos fases (de la moneda origen a EUR, y de EUR a la moneda 
 destino), optimizando el uso de la API externa que solo proporciona tasas relativas al Euro.
 
 Integración con API externa de tasas de cambio:
 Realizar una integración robusta con la API externa de Exchange Rates para obtener las tasas de cambio actualizadas, 
 que son la base para todos los cálculos de conversión. 
 
* Manejo centralizado y detallado de errores:
 Implementa un manejo personalizado de excepciones (`CurrencyNotFoundException`, `ExternalApiException`) para escenarios 
 específicos como monedas no soportadas o fallos de la API externa.
 Utiliza un `@RestControllerAdvice` global para centralizar la gestión de errores, interceptando todas las excepciones 
 lanzadas por los controladores.
 Asigna respuestas HTTP estandarizadas y significativas según el tipo de error (ej., 400 Bad Request para validación, 
 404 Not Found para divisas no soportadas, 503 Service Unavailable para fallos externos, 500 Internal Server Error para 
 errores inesperados), lo que garantiza una API más limpia, escalable y consistente.
 
* Mecanismo de Caché en memoria para tasas de cambio: 
 Para optimizar el rendimiento y reducir la dependencia de la API externa, las tasas de cambio obtenidas se almacenan 
 temporalmente en una caché en memoria. 
 
* Sistema de notificación de fallos con Cooldown:
 La implementación en LoggingNotificationAdapter es robusta y escalable, incorporando un mecanismo de cooldown 
 de 5 minutos para las alertas. Esto previene la saturación de notificaciones ante fallos continuos de servicios externos, 
 permitiendo al equipo de operaciones enfocarse en la solución de la causa raíz. 
 El proceso de notificaciones se ejecuta de forma asíncrona, garantizando que no bloquee el hilo principa de la aplicación 
 y permitiendo una experiencia de usuario ininterrumpida y una mayor escalabilidad bajo carga.

* Validación exhaustiva de entradas de la API.
 Se realiza una validación exhaustiva de las entradas, revisando un rango de entrada  válido para el dato numérico de
 entrada del monto a convertir, y si las monedas de origen y destino a convertir cumplen con estándar ISO 4217.

* Testing y pruebas unitarias:
 Se verifica los casos de éxito (conversión entre euros y otra moneda, y entre dos monedas diferente a euro) y los casos
 de fallo (moneda no encontrada). 
 
* Métricas de rendimiento y observabilidad con Micrometer (éxitos, fallos, duración de conversión).
 Se han implementado contadores para registrar las conversiones exitosas y fallidas, y un temporizador para medir la 
 duración de las operaciones de conversión.
 Estas métricas son cruciales para monitorear el rendimiento y la salud de la aplicación en entornos de producción.
 Para las pruebas unitarias, se utiliza SimpleMeterRegistry, una implementación ligera que no requiere un servidor Prometheus real.
 
* Comunicación Segura (HTTPS): 
 La aplicación está configurada para servir la API a través de HTTPS, utilizando un certificado auto-firmado, lo que 
 demuestra la preocupación por la seguridad en la comunicación. La contraseña de server.ssl.key-store-password que se 
 tiene en application.prod.properties idealmente en producción debería ser una variable de entorno o gestionada por un 
 sistema de secretos, no hardcodeada. Por ejemplo: server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}.
 
 Monitoreo y Operaciones con Spring Boot Actuator: 
 Se exponen los endpoints health e info de Spring Boot Actuator, esenciales para verificar el estado de la aplicación, 
 obtener metadatos y facilitar la integración con herramientas de monitoreo en entornos de producción.
 
* Contenedorización con Docker:
 Se proporciona un Dockerfile para empaquetar la aplicación en un contenedor aislado, facilitando su despliegue 
 consistente en cualquier entorno (desarrollo, pruebas, producción) sin preocuparse por las dependencias del sistema 
 host. Además, se demuestra la gestión segura de las claves API a través de variables de entorno de Docker.
 
 Dejamos el access key del API externa definido como variable de entorno, 
 y la seteamos desde el llamado de docker run:
 docker run -p 8080:8080 -e EXCHANGE_RATES_API_KEY=10124780aa73c83cd1e5b667cf8af774 my-currency-converter:latest
 De esta forma mantenemos los secrets y la información sensible fuera de git y de ser hardcodeado en el código y los 
 archivos de configuración. 

### 3. Decisiones de Diseño y Arquitectura

* **Arquitectura:** 
Se optó por esta arquitectura para lograr una separación clara de preocupaciones (Application, Infrastructure, 
Presentation, Domain), lo que resulta en una aplicación más mantenible, testable y desacoplada. La capa de aplicación 
actúa como el orquestador principal, utilizando puertos (interfaces) y adaptadores para interactuar con la 
infraestructura. Las diferentes capas se comunican mediante interfaces, garantizando la inversión de dependencias y 
una estructura modular.

* Los DTOs (`ConversionRequest`, `ConversionResponse`) y las excepciones (CurrencyNotFoundException, ExternalApiException, 
* GlobalExceptionHandler) se encuentran en sus capas correspondientes (presentación e infraestructura), manteniendo la 
* separación de responsabilidades.

* Esta estructura facilita la adaptación del sistema a futuras reglas de negocio o funcionalidades complejas, como 
* gestión de usuarios, historiales de conversiones, comisiones dinámicas, o integración con múltiples fuentes de datos.
 
* **Patrones de Diseño Clave:**
    * **Strategy Pattern:** 
    Implementado en `ExchangeRateProviderStrategy` para permitir cambiar fácilmente la fuente de las tasas de cambio (ej. API externa, base de datos, archivo).
    * **Adapter Pattern:** 
    `ExchangeRateApiAdapter` y `InMemoryExchangeRateCacheAdapter` actúan como adaptadores para los puertos de salida.
    * **Builder Pattern:** 
    Utilizado en `ConversionResponse` DTO para una construcción de objetos más legible y flexible.
* **Manejo de Precisión Financiera (`BigDecimal`):** 
 Se utiliza `BigDecimal` para todas las operaciones monetarias y tasas de cambio. Esta decisión es crítica para evitar 
 problemas de precisión inherentes a `Double` o `Float`, garantizando la exactitud requerida en  cálculos financieros 
 exactos.
* **Métricas y Observabilidad (`Micrometer`):** 
 Implementación de contadores de éxitos/fallos y un temporizador para la duración de las conversiones, esencial para monitorear la salud y el rendimiento en producción.
* **Manejo de Errores y Notificaciones:**
    * Excepciones personalizadas (`CurrencyNotFoundException`, `ExternalApiException`) para un control granular. `CurrencyNotFoundException` se maneja en escenarios donde una moneda no es soportada por la API externa, `ExternalApiException`se utiliza
    para errores de conexión con la API externa. 
    * `GlobalExceptionHandler` para centralizar el manejo de errores REST y proporcionar respuestas uniformes.
    * `NotificationService` con un adaptador de log (`LoggingNotificationAdapter`) y un mecanismo de `cooldown` para evitar la saturación de alertas, crucial para operaciones. Las notificaciones son asíncronas para no bloquear la ejecución principal.
* **Contenedorización (`Docker`):** 
Provisión de un `Dockerfile` para empaquetar la aplicación y sus dependencias, facilitando el despliegue consistente en cualquier entorno.
* **Gestión de Perfiles (`Spring Profiles`):** 
EL uso de `application-prod.properties` para definir configuraciones específicas de producción, como niveles de log y URLs de API, demostrando así una práctica estándar para gestionar entornos, separando eficientemente las configuraciones de desarrollo y producción.
* **Dependencias de Spring:** 
Se hace uso estratégico de componentes clave de Spring como `RestTemplate` para llamadas HTTP externas, `@Value` para inyección de propiedades, y anotaciones como `@Service`, `@Component`, `@RestController`, `@Autowired` (o inyección por constructor) para una gestión
eficiente de la inversión de control y las dependencias. 

### 4. Tecnologías Utilizadas
* Se elige Spring Boot como framework principal por su agilidad en el desarrollo de microservicios, la madurez de su ecosistema,
* facilidad para implementar patrones de diseño y soporte robusto para pruebas y observabilidad. 
*  
* **Tech Stack** 
* Java 17
* Spring Boot (versión 3.5.0)
* Maven (versión 3.9.9)
* Micrometer (para métricas)
* Lombok (para simplificar DTOs)
* JUnit 5 & Mockito (para pruebas)
* Exchange Rates API (API externa de tasas de cambio)
* Docker

### 5. Requisitos
* JDK 17 o superior
* Maven 3.6.x o superior
* Docker (opcional, para ejecutar en contenedor)
* Conexión a internet para la API externa.

### 6. Cómo Compilar y Ejecutar

#### Localmente
1.  Clonar el repositorio: `git clone [URL_REPOSITORIO]`
2.  Navegar al directorio del proyecto: `cd currency-converter`
3.  Compilar el proyecto: `mvn clean install`
4. **Configurar la variable de entorno `EXCHANGE_RATES_API_KEY`:**

    * **Windows (Command Prompt):**
      ```cmd
      set EXCHANGE_RATES_API_KEY="10124780aa73c83cd1e5b667cf8af774"
      ```
    * **Linux/macOS (Bash/Zsh):**
      ```bash
      export EXCHANGE_RATES_API_KEY="10124780aa73c83cd1e5b667cf8af774"
      ```
      *(Este paso es necesario para que la aplicación pueda acceder a la API externa.)*
5. HTTP (puerto 8080): `java -jar target/currency-converter-0.0.1-SNAPSHOT.jar`
    HTTPS (puerto 8443): `java -jar target/currency-converter-0.0.1-SNAPSHOT.jar` (se ejecutará automáticamente en HTTPS si está configurado el puerto 8443 en application.properties).
    Para ejecutar con perfil de producción: `java -jar target/currency-converter-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

#### Con Docker
1.  Tener Docker instalado y ejecutándose.
2.  Navega al directorio raíz del proyecto.
3.  Construye la imagen Docker: `docker build -t my-currency-converter:latest .`
4.  Ejecuta el contenedor: `docker run -p 8080:8080 -e EXCHANGE_RATES_API_KEY=10124780aa73c83cd1e5b667cf8af774 my-currency-converter:latest`
    * Para ejecutar con **perfil de producción y pasar la clave API como variable de entorno (usando HTTPS por defecto):**
        ```bash
        docker run -p 8443:8443 -e SPRING_PROFILES_ACTIVE=prod -e EXCHANGE_RATES_API_KEY=10124780aa73c83cd1e5b667cf8af774 my-currency-converter
        ```
      La aplicación estará disponible en `http://localhost:8080` según la configuración y el perfil activo.
### 7. Endpoints de la API
* **POST /api/v1/convert**
    * **Descripción:** Convierte un monto de una divisa origen a una divisa destino.
    * **Body (JSON):**
        ```json
        {
            "amount": 100.00,
            "sourceCurrency": "USD",
            "targetCurrency": "GBP"
        }
        ```
    * **Ejemplo de Respuesta Exitosa (200 OK):**
        ```json
        {
            "conversionDate": "2025-06-02",
            "exchangeRate": 0.787037037037037037037037037037037,
            "originalAmount": 100.00,
            "convertedAmount": 78.7037037037037037037037037037037,
            "sourceCurrency": "USD",
            "targetCurrency": "GBP"
        }
        ```
    * **Ejemplo de Respuesta de Error (400 Bad Request - Validación):**
        ```json
        {
            "amount": "Amount must be greater than zero"
        }
        ```
    * **Ejemplo de Respuesta de Error (404 Not Found - Moneda no soportada):**
        ```json
        {
            "error": "Source currency not supported: XXX"
        }
        ```
    * **Ejemplo de Respuesta de Error (503 Service Unavailable - API Externa Falló):**
        ```json
        {
            "error": "An issue occurred with the external exchange rate service. Please try again later."
        }
        ```

### 8. Pruebas
* Ejecuta las pruebas unitarias y de integración con Maven:
`mvn test` o `mvn clean test jacoco:report` para revisión de cobertura de código.

### 9. Próximos Pasos / Mejoras Potenciales
* Este proyecto ha establecido una base sólida para futuras mejoras y expansiones:
 
 Persistencia de Tasas de Cambio: Integrar una base de datos (SQL o NoSQL) para almacenar y cachear tasas de cambio a 
 largo plazo.
 
* Múltiples Proveedores de Tasas de Cambio: Implementar más adaptadores para diferentes APIs de tasas de cambio,
 añadiendo lógica de fallback robusta. 
 
* Límites de Cuota y Control de Flujo: Implementar mecanismos para gestionar límites de llamadas a APIs externas. 
 
* Autenticación y Autorización: Añadir seguridad a los endpoints de la API (ej., OAuth2, JWT). 
Integración con Kafka/RabbitMQ: Utilizar un sistema de mensajería para notificaciones asíncronas o procesamiento 
de transacciones. 
 
* Alertas a Herramientas de Monitoreo: Integrar las métricas de Micrometer con Prometheus y Grafana para un monitoreo 
y visualización más avanzados. 

* Despliegue Continuo (CI/CD): Configurar pipelines de CI/CD (ej., Jenkins, GitHub Actions) para automatizar pruebas 
y despliegues. 
 
* Documentación de API: Generar documentación de la API automáticamente con OpenAPI/Swagger.

### 10. Contacto
Oscar Daniel Guzmán Neira - https://www.linkedin.com/in/oscar-daniel-guzman/