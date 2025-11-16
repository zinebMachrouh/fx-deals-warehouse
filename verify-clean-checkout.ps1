# Clean Checkout Verification Script (PowerShell)
# This script demonstrates that all tests can run from a clean checkout
# without any manual steps.

$ErrorActionPreference = "Stop"

Write-Host "=========================================="
Write-Host "FX Deals Warehouse - Clean Checkout Test"
Write-Host "=========================================="
Write-Host ""

# Step 1: Verify Maven wrapper exists
Write-Host "Step 1: Verifying Maven wrapper..."
if (Test-Path "./mvnw.cmd") {
    Write-Host "✓ Maven wrapper found" -ForegroundColor Green
} else {
    Write-Host "✗ Maven wrapper not found" -ForegroundColor Red
    exit 1
}

# Step 2: Clean any existing build artifacts
Write-Host ""
Write-Host "Step 2: Cleaning build artifacts..."
& ./mvnw.cmd clean -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Clean failed" -ForegroundColor Red
    exit 1
}

# Step 3: Run all tests with coverage
Write-Host ""
Write-Host "Step 3: Running tests with coverage verification..."
& ./mvnw.cmd verify -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "✗ Tests failed" -ForegroundColor Red
    exit 1
}

# Step 4: Verify coverage report was generated
Write-Host ""
Write-Host "Step 4: Verifying coverage report..."
if (Test-Path "target/site/jacoco/index.html") {
    Write-Host "✓ Coverage report generated at: target/site/jacoco/index.html" -ForegroundColor Green
} else {
    Write-Host "✗ Coverage report not found" -ForegroundColor Red
    exit 1
}

# Step 5: Verify JAR was built
Write-Host ""
Write-Host "Step 5: Verifying JAR artifact..."
if (Test-Path "target/fx-deals-warehouse-0.0.1-SNAPSHOT.jar") {
    Write-Host "✓ JAR built successfully: target/fx-deals-warehouse-0.0.1-SNAPSHOT.jar" -ForegroundColor Green
} else {
    Write-Host "✗ JAR not found" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=========================================="
Write-Host "✓ All tests passed successfully!" -ForegroundColor Green
Write-Host "=========================================="
Write-Host ""
Write-Host "Coverage report: target/site/jacoco/index.html"
Write-Host "Test results: target/surefire-reports/"
Write-Host ""

