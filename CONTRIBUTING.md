# Contributing to Besu Service Optimization

Thank you for your interest in contributing to this project! This document provides guidelines for contributing.

## How to Contribute

### Reporting Issues

- Check if the issue already exists
- Use the issue template if available
- Include:
  - Clear description of the problem
  - Steps to reproduce
  - Expected vs actual behavior
  - Environment details (OS, Java version, Node.js version)

### Submitting Pull Requests

1. **Fork** the repository
2. **Create a branch** for your feature or fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes** following the code style guidelines
4. **Test your changes** locally
5. **Commit** with clear messages:
   ```bash
   git commit -m "Add: brief description of changes"
   ```
6. **Push** to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
7. **Create a Pull Request** to the `main` branch

## Code Style

### Java (Backend)

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Add Javadoc for public methods
- Keep methods small and focused

### TypeScript (Middleware)

- Use TypeScript strict mode
- Follow ESLint rules
- Use `async/await` instead of callbacks
- Add JSDoc comments for exported functions

## Development Setup

### Backend (Java)

```bash
cd backend
./gradlew build
./gradlew test
./gradlew bootRun
```

### Middleware (Node.js)

```bash
cd middleware
npm install
npm run build
npm start
```

### Full Stack (Docker)

```bash
cd deployment/local
docker compose up -d
```

## Testing

- Write unit tests for new features
- Ensure existing tests pass before submitting PR
- For Java: `./gradlew test`
- For Node.js: `npm test`

## Commit Message Convention

Use clear, descriptive commit messages:

- `Add:` for new features
- `Fix:` for bug fixes
- `Update:` for changes to existing features
- `Refactor:` for code refactoring
- `Docs:` for documentation changes
- `Test:` for adding tests

Example:
```
Add: Transaction Isolation Pattern for DB connection optimization
```

## Questions?

If you have questions, feel free to:
- Open an issue with the `question` label
- Contact the maintainers

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
