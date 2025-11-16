#!/bin/bash

# Clean Checkout Verification Script
# This script demonstrates that all tests can run from a clean checkout
# without any manual steps.

set -e  # Exit on any error

echo "=========================================="
echo "FX Deals Warehouse - Clean Checkout Test"
echo "=========================================="
echo ""

# Step 1: Verify Maven wrapper exists
echo "Step 1: Verifying Maven wrapper..."
if [ -f "./mvnw.cmd" ]; then
    echo "✓ Maven wrapper found"
else
    echo "✗ Maven wrapper not found"
    exit 1
fi

# Step 2: Clean any existing build artifacts
echo ""
echo "Step 2: Cleaning build artifacts..."
./mvnw.cmd clean -q

# Step 3: Run all tests with coverage
echo ""
echo "Step 3: Running tests with coverage verification..."
./mvnw.cmd verify -q

# Step 4: Verify coverage report was generated
echo ""
echo "Step 4: Verifying coverage report..."
if [ -f "target/site/jacoco/index.html" ]; then
    echo "✓ Coverage report generated at: target/site/jacoco/index.html"
else
    echo "✗ Coverage report not found"
    exit 1
fi

# Step 5: Verify JAR was built
echo ""
echo "Step 5: Verifying JAR artifact..."
if [ -f "target/fx-deals-warehouse-0.0.1-SNAPSHOT.jar" ]; then
    echo "✓ JAR built successfully: target/fx-deals-warehouse-0.0.1-SNAPSHOT.jar"
else
    echo "✗ JAR not found"
    exit 1
fi

echo ""
echo "=========================================="
echo "✓ All tests passed successfully!"
echo "=========================================="
echo ""
echo "Coverage report: target/site/jacoco/index.html"
echo "Test results: target/surefire-reports/"
echo ""

