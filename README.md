# Currency Converter API

¡Bienvenido al proyecto Currency Converter API! Esta es una aplicación Spring Boot que proporciona un servicio de conversión de divisas, implementando principios de Clean Architecture y patrones de diseño para una solución robusta, escalable y mantenible.

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
* La aplicación tiene como propósito realizar un conversor de monedas
* que funciona consultando en una API externa las tasas de cambio con 
* base al euro (EUR), con lo que se busca dar una demostración de como 
* establecer una base sólida para crear un proyecto escalable y mantenible,
* asegurando así ser adaptable y resiliente a cambios y errores, más cercano 
* a un escenario con un dominio de negocio real. 

### 2. Características
* Conversión de divisas (Origen -> EUR -> Destino):
* Se emplea una lógica de conversión de dos pasos (Origen -> EUR -> Destino) se maneja
* explícitamente, ya que la API externa proporciona tasas relativas a EUR.
* 
* Integración con API externa de tasas de cambio:
* Realizamos una integración con la API externa de Exchange Rates para obtener las tasas de cambio y posteriormente 
* hacer el cálculo del cambio a la moneda destino. 
* 
* Manejo de errores para monedas no soportadas y fallos de la API externa:
* Se tiene un manejo personalizado de errores en caso de no encontrar una moneda soportada o si hay algún fallo presente 
* en la API externa. Adicionalmente, se implementa un manejo de errores centralizado mediante `@RestControllerAdivce` que 
* permite interceptar todas las excepciones lanzadas por los controladores. 
* Mecanismo de caché en memoria para tasas de cambio. A través de métodos anotados con @ExceptionHandler, se asignan 
* respuestas HTTP adecuadas según el tipo de error (400 Bad Request para validación, 404 Not Found para moneda no soportada,
* 503 Service Unavailable para fallos de la API externa, 500 Internal Server Error para errores inesperados). 
* Esto proporciona una solución de manejo de errores mucho más limpia escalable y consistente para el API.
* 
* Sistema de notificación de fallos de servicios externos con cooldown.
* La implementación de la lógica de notificación en LoggingNotificationAdapter es robusta y escalable, pues cuenta con un 
* mecanismo de ‘cooldown’ de 5 minutos para las alertas. Esto significa que si un servicio externo experimenta fallos continuos, 
* solo se enviará una notificación inicial y luego se esperara´un período definido antes de enviar otra notificación para 
* el mismo problema. Esto con el fin de evitar saturar las alertas al equipo de operaciones, permitiéndoles centrarse en 
* la causa raíz sin ser expuestos a mensajes repetitivos, mejorando así la gestión de incidentes.

Adicionalmente el proceso de notificaciones se ejecuta de forma asíncrona. Esto garantiza que las operaciones de 
notificación no bloqueen el hilo principal de la aplicación. La aplicación garantiza una experiencia de usuario sin 
interrupciones ante operaciones secundarias, y permite que el sistema escale eficientemente bajo carga, sin que la capa 
de presentación se vea afectada por el rendimiento del sistema de notificaciones.
* 
* Testing y pruebas unitarias:
* Se verifica los casos de éxito (conversión entre euros y otra moneda, y entre dos monedas diferente a euro) y los casos
* de fallo (moneda no encontrada). 
* 
* Métricas de rendimiento con Micrometer (éxitos, fallos, duración de conversión).
* Las métricas  (Counter y Timer) se incrementan correctamente en los escenarios de éxito y fallo. Utilizando 
* SimpleMeterRegistry para las pruebas unitarias de métricas, ya que es ligero y no requiere un servidor Prometheus real.
* 
* 
* Validación de entradas de la API.
* Se realiza una validación exhaustiva de las entradas, revisando un rango de entrada  válido para el dato numérico de
* entrada del monto a convertir, y si las monedas de origen y destino a convertir cumplen con estándar ISO 4217. 
*  
* Contenedorización con Docker.
* Creamos un Dockerfile para poner nuestro microservicio en un contenedor y poder correrlo luego en un ambiente de desarrollo
* o de producción, y para facilitar su despliegue y pruebas desde cualquier dispositivo sin preocupación de tener que 
* instalar las dependencias en el ambiente local. Dejamos el access key del API externa definido como variable de entorno, 
* y la seteamos desde el llamado de docker run:
* docker run -p 8080:8080 -e EXCHANGE_RATES_API_KEY=10124780aa73c83cd1e5b667cf8af774 my-currency-converter:latest
* De esta forma mantenemos los secrets y la información sensible fuera de git y de ser hardcodeado en el código y los 
* archivos de configuración. 

### 3. Decisiones de Diseño y Arquitectura

* **Arquitectura:** 
* Se optó por una arquitectura hexagonal (Clean Architecture) 
* para separar las preocupaciones y facilitar la mantenibilidad 
* y testabilidad. 
* Esto se refleja en la estructura de paquetes (Application, 
* Infrastructure, Presentation, Domain).
* En cambio, la capa de aplicación actúa como el orquestador principal, utilizando los puertos (ports) y adaptadores 
* (adapters) de la capa de infraestructura (infrastructure) para obtener las tasas de cambio y realizar el cálculo directamente.
*
* Las diferentes capas se comunican mediante interfaces (el controlador con el servicio, con el adaptador, etc), garantizando
* la inversión de dependencias y una estructura modular y desacoplada. 
*
* Los DTOs (ConversionRequest, ConversionResponse) y las excepciones (CurrencyNotFoundException, ExternalApiException, 
* GlobalExceptionHandler) se manejan en sus capas correspondientes (presentación e infraestructura, respectivamente), 
* manteniendo la separación de responsabilidades.
*
* Con esta estructura es viable realizar una adaptación a un sistema con unas reglas negocio aplicables cuando el proyecto
* evolucione y se añaden funcionalidades como gestión de usuarios y perfiles, historiales de conversiones, reglas de negocio 
* complejas (por ej, límites de conversión, comisiones dinámicas, integración con sistemas contables, etc), o múltiples 
* fuentes de datos que necesiten ser consolidadas o validadas de forma compleja.
*
* 
* **Patrones de Diseño Clave:**
    * **Strategy Pattern:** 
    * Implementado en `ExchangeRateProviderStrategy` para permitir cambiar fácilmente la fuente de las tasas de cambio (ej. API externa, base de datos, archivo).
    * **Adapter Pattern:** 
    * `ExchangeRateApiAdapter` y `InMemoryExchangeRateCacheAdapter` actúan como adaptadores para los puertos de salida.
    * **Builder Pattern:** 
    * Utilizado en `ConversionResponse` DTO para una construcción de objetos más legible y flexible.
* **Manejo de Precisión de Dinero (`BigDecimal`):** 
* Se utiliza `BigDecimal` para todas las operaciones monetarias y tasas de cambio para evitar problemas de precisión inherentes a `Double` o `Float`, asegurando cálculos financieros exactos.
* **Métricas y Observabilidad (`Micrometer`):** 
* Implementación de contadores de éxitos/fallos y un temporizador para la duración de las conversiones, esencial para monitorear la salud y el rendimiento en producción.
* **Manejo de Errores y Notificaciones:**
    * Excepciones personalizadas (`CurrencyNotFoundException`, `ExternalApiException`) para un control granular. `CurrencyNotFoundException` se maneja en escenarios donde una moneda no es soportada por la API externa, `ExternalApiException`se utiliza
    * para errores de conexión con la API externa. 
    * `GlobalExceptionHandler` para centralizar el manejo de errores REST y proporcionar respuestas uniformes.
    * `NotificationService` con un adaptador de log (`LoggingNotificationAdapter`) y un mecanismo de `cooldown` para evitar la saturación de alertas, crucial para operaciones. Las notificaciones son asíncronas para no bloquear la ejecución principal.
* **Contenedorización (`Docker`):** 
* Provisión de un `Dockerfile` para empaquetar la aplicación y sus dependencias, facilitando el despliegue consistente en cualquier entorno.
* **Gestión de Perfiles (`Spring Profiles`):** 
* Uso de `application-prod.properties` para definir configuraciones específicas de entorno de producción, como niveles de log y URLs de API, separando así las configuraciones de desarrollo y producción.
* **Dependencias de Spring:** 
* Se emplea `RestTemplate` para llamadas HTTP, `@Value` para inyección de propiedades, `@Service`, `@Component`, `@RestController`, `@Autowired` (o inyección por constructor).

### 4. Tecnologías Utilizadas
* Se elige como framework principal Spring Boot por su agilidad en el desarrollo de microservicios, madurez en el ecosistema, facilidad 
* para implementar patrones y soporte robusto para las pruebas y observabilidad. 
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
1.  Clona el repositorio: `git clone [URL_REPOSITORIO]`
2.  Navega al directorio del proyecto: `cd currency-converter`
3.  Compila el proyecto: `mvn clean install`
4.  Ejecuta la aplicación: `java -jar target/currency-converter-0.0.1-SNAPSHOT.jar`
    * Para ejecutar con perfil de producción: `java -jar target/currency-converter-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod`

#### Con Docker
1.  Asegurar tener Docker instalado y ejecutándose.
2.  Navega al directorio raíz del proyecto.
3.  Construye la imagen Docker: `docker build -t currency-converter-app .`
4.  Ejecuta el contenedor: `docker run -p 8080:8080 currency-converter-app`
    * Para ejecutar con perfil de producción: `docker run -p 8080:8080 -e "SPRING_PROFILES_ACTIVE=prod" currency-converter-app`
      La aplicación estará disponible en `http://localhost:8080`.

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
Ejecuta las pruebas unitarias y de integración con Maven:
`mvn test`

### 9. Próximos Pasos / Mejoras Potenciales
Ideas futuras para considerar para el crecimiento del proyecto.

### 10. Contacto
Oscar Daniel Guzmán Neira - https://www.linkedin.com/in/oscar-daniel-guzman/