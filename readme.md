# ZapVideo

## Project Overview
ZapVideo is a tool designed to download videos for sharing via WhatsApp, utilizing Java, HTML, and CSS, with Github Actions and Nginx as a reverse proxy.

## Getting Started

### Prerequisites
- Java 23
- Gradle
- Git

### Installation
1. Clone the repository:
    ```sh
    git clone https://github.com/jgmacedo/ZapVideo.git
    cd ZapVideo
    ```

2. Build the project:
    ```sh
    ./gradlew clean build
    ```

### Running the Application
To run the application locally:
```sh
./gradlew bootRun
```

Deployment
The application is deployed automatically using GitHub Actions. The deployment script is located in `.github/workflows/newdeploy.yml`.

Usage
Once the application is running, you can access it at `www.zapvideo.site`.

### API Documentation
#### Endpoints
- `GET /videos`: Retrieve a list of videos.
- `POST /videos`: Upload a new video.
- `DELETE /videos/{id}`: Delete a video by ID.

#### Example Request
```sh
curl -X POST http://localhost:8080/videos -F "file=@/path/to/video.mp4"
```

### Contributing
1. Fork the repository.
2. Create a new branch (`git checkout -b feature-branch`).
3. Make your changes.
4. Commit your changes (`git commit -m 'Add some feature'`).
5. Push to the branch (`git push origin feature-branch`).
6. Open a pull request.

### License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Reference Documentation
For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/3.4.2/gradle-plugin)
* [Create an OCI image](https://docs.spring.io/spring-boot/3.4.2/gradle-plugin/packaging-oci-image.html)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/3.4.2/reference/actuator/index.html)
* [Spring Web](https://docs.spring.io/spring-boot/3.4.2/reference/web/servlet.html)
* [Spring Boot DevTools](https://docs.spring.io/spring-boot/3.4.2/reference/using/devtools.html)
* [Validation](https://docs.spring.io/spring-boot/3.4.2/reference/io/validation.html)
* [Rest Repositories](https://docs.spring.io/spring-boot/3.4.2/reference/data-rest-repositories.html)
