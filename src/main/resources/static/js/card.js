/**
 * ========================================
 * CREDIT CARD HANDLER - HealthFirst
 * Random Background Selection & Validation
 * ========================================
 */

document.addEventListener('DOMContentLoaded', function() {
    // Configuration
    const TOTAL_CARD_BACKGROUNDS = 25;
    const IMAGE_BASE_PATH = '/assets/images/';
    const IMAGE_EXTENSION = '.jpeg';
    const MAX_CARDHOLDER_NAME_LENGTH = 26;

    // Fallback gradients if images don't load
    const FALLBACK_GRADIENTS = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #30cfd0 0%, #330867 100%)',
        'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
        'linear-gradient(135deg, #ff9a56 0%, #ff6a88 100%)'
    ];

    // DOM Elements
    const cardNumberInput = document.getElementById('cardNumber');
    const cardNameInput = document.getElementById('cardName');
    const cardExpiryInput = document.getElementById('cardExpiry');
    const cardCvvInput = document.getElementById('cardCvv');
    const creditCard = document.getElementById('creditCard');

    const cardNumberDisplay = document.getElementById('cardNumberDisplay');
    const cardNameDisplay = document.getElementById('cardNameDisplay');
    const cardExpiryDisplay = document.getElementById('cardExpiryDisplay');
    const cardCvvDisplay = document.getElementById('cardCvvDisplay');
    const cardTypeLogo = document.getElementById('cardTypeLogo');

    const cardBgFront = document.getElementById('cardBgFront');
    const cardBgBack = document.getElementById('cardBgBack');
    const cardFront = document.querySelector('.card-front');
    const cardBack = document.querySelector('.card-back');

    // Card Type Logos
    const cardLogos = {
        visa: 'https://img.icons8.com/color/48/visa.png',
        mastercard: 'https://img.icons8.com/color/48/mastercard-logo.png',
        amex: 'https://img.icons8.com/color/48/amex.png',
        discover: 'https://img.icons8.com/color/48/discover.png',
        diners: 'https://img.icons8.com/color/48/diners-club.png',
        jcb: 'https://img.icons8.com/color/48/jcb.png',
        default: 'https://img.icons8.com/color/48/bank-card-back-side.png'
    };

    /**
     * Initialize card with random background
     */
    function initializeCard() {
        const randomImageNumber = Math.floor(Math.random() * TOTAL_CARD_BACKGROUNDS) + 1;
        const imagePath = `${IMAGE_BASE_PATH}${randomImageNumber}${IMAGE_EXTENSION}`;

        console.log(`🎨 Attempting to load card background: ${imagePath}`);
        loadCardBackground(imagePath, randomImageNumber);
    }

    /**
     * Load card background with fallback
     */
    function loadCardBackground(imagePath, imageNumber) {
        const testImg = new Image();

        testImg.onload = function() {
            console.log(`✓ Background loaded successfully: ${imagePath}`);
            cardBgFront.src = imagePath;
            cardBgBack.src = imagePath;
            cardBgFront.style.display = 'block';
            cardBgBack.style.display = 'block';
        };

        testImg.onerror = function() {
            console.warn(`✗ Failed to load image: ${imagePath}, using gradient fallback`);
            useFallbackGradient(imageNumber);
        };

        testImg.src = imagePath;
    }

    /**
     * Use gradient fallback when image doesn't load
     */
    function useFallbackGradient(imageNumber) {
        cardBgFront.style.display = 'none';
        cardBgBack.style.display = 'none';

        const gradientIndex = (imageNumber - 1) % FALLBACK_GRADIENTS.length;
        const gradient = FALLBACK_GRADIENTS[gradientIndex];

        cardFront.style.background = gradient;
        cardBack.style.background = gradient;

        console.log(`✓ Applied fallback gradient`);
    }

    /**
     * Detect card type based on number
     */
    function detectCardType(number) {
        const patterns = {
            visa: /^4/,
            mastercard: /^5[1-5]/,
            amex: /^3[47]/,
            discover: /^(6011|65|64[4-9])/,
            diners: /^3(0[0-5]|[68])/,
            jcb: /^35/
        };

        for (let type in patterns) {
            if (patterns[type].test(number)) {
                return type;
            }
        }
        return 'default';
    }

    /**
     * Format card number with spaces
     */
    function formatCardNumber(value) {
        const cleaned = value.replace(/\s/g, '').replace(/[^0-9]/gi, '');
        const formatted = cleaned.match(/.{1,4}/g);
        return formatted ? formatted.join(' ') : cleaned;
    }

    /**
     * Mask card number for display
     */
    function maskCardNumber(value) {
        const cleaned = value.replace(/\s/g, '');

        if (cleaned.length === 0) {
            return '•••• •••• •••• ••••';
        }

        const groups = cleaned.match(/.{1,4}/g) || [];
        const displayGroups = groups.map((group, index) => {
            if (index > 0 && index < groups.length - 1) {
                return '••••';
            }
            return group.padEnd(4, '•');
        });

        while (displayGroups.length < 4) {
            displayGroups.push('••••');
        }

        return displayGroups.join(' ');
    }

    /**
     * Card Number Input Handler
     */
    cardNumberInput.addEventListener('input', function(e) {
        const formatted = formatCardNumber(e.target.value);
        e.target.value = formatted;

        const masked = maskCardNumber(e.target.value);
        cardNumberDisplay.textContent = masked;

        const cardType = detectCardType(e.target.value.replace(/\s/g, ''));
        cardTypeLogo.src = cardLogos[cardType];
    });

    /**
     * Card Name Input Handler
     */
    cardNameInput.addEventListener('input', function(e) {
        let value = e.target.value;

        if (value.length > MAX_CARDHOLDER_NAME_LENGTH) {
            value = value.substring(0, MAX_CARDHOLDER_NAME_LENGTH);
            e.target.value = value;
        }

        value = value.replace(/[^a-zA-Z\s\-']/g, '');
        e.target.value = value;

        const displayValue = value.toUpperCase();
        cardNameDisplay.textContent = displayValue || 'YOUR NAME';
    });

    /**
     * Expiry Date Input Handler
     */
    cardExpiryInput.addEventListener('input', function(e) {
        let value = e.target.value.replace(/[^0-9]/gi, '');

        if (value.length >= 2) {
            value = value.substring(0, 2) + '/' + value.substring(2, 4);
        }

        e.target.value = value;
        cardExpiryDisplay.textContent = value || '••/••';
    });

    /**
     * CVV Input Handlers - Flip Card (NO ANIMATION CONTROL)
     */
    cardCvvInput.addEventListener('focus', function() {
        creditCard.classList.add('is-flipped');
    });

    cardCvvInput.addEventListener('blur', function() {
        creditCard.classList.remove('is-flipped');
    });

    cardCvvInput.addEventListener('input', function(e) {
        const value = e.target.value.replace(/[^0-9]/gi, '');
        e.target.value = value;
        cardCvvDisplay.textContent = '•'.repeat(value.length) || '•••';
    });

    /**
     * Luhn Algorithm
     */
    function luhnCheck(cardNumber) {
        let sum = 0;
        let isEven = false;

        for (let i = cardNumber.length - 1; i >= 0; i--) {
            let digit = parseInt(cardNumber[i]);

            if (isEven) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            isEven = !isEven;
        }

        return sum % 10 === 0;
    }

    /**
     * Validate expiry date
     */
    function validateExpiryDate(expiry) {
        if (!/^\d{2}\/\d{2}$/.test(expiry)) {
            return { valid: false, message: 'Invalid expiry date format (MM/YY)' };
        }

        const [month, year] = expiry.split('/').map(num => parseInt(num));

        if (month < 1 || month > 12) {
            return { valid: false, message: 'Invalid month' };
        }

        const currentDate = new Date();
        const currentYear = currentDate.getFullYear() % 100;
        const currentMonth = currentDate.getMonth() + 1;

        if (year < currentYear || (year === currentYear && month < currentMonth)) {
            return { valid: false, message: 'Card has expired' };
        }

        return { valid: true };
    }

    /**
     * Form Validation
     */
    function validateForm() {
        const cardNumber = cardNumberInput.value.replace(/\s/g, '');
        const cardName = cardNameInput.value.trim();
        const cardExpiry = cardExpiryInput.value;
        const cardCvv = cardCvvInput.value;
        const billingAddress = document.getElementById('billingAddress').value.trim();
        const billingCity = document.getElementById('billingCity').value.trim();
        const billingZip = document.getElementById('billingZip').value.trim();

        if (cardNumber.length < 13 || cardNumber.length > 19) {
            alert('Please enter a valid card number (13-19 digits)');
            cardNumberInput.focus();
            return false;
        }

        if (!luhnCheck(cardNumber)) {
            alert('Invalid card number. Please check and try again.');
            cardNumberInput.focus();
            return false;
        }

        if (cardName.length < 2) {
            alert('Please enter the cardholder name (minimum 2 characters)');
            cardNameInput.focus();
            return false;
        }

        const expiryValidation = validateExpiryDate(cardExpiry);
        if (!expiryValidation.valid) {
            alert(expiryValidation.message);
            cardExpiryInput.focus();
            return false;
        }

        if (cardCvv.length < 3 || cardCvv.length > 4) {
            alert('Please enter a valid CVV (3-4 digits)');
            cardCvvInput.focus();
            return false;
        }

        if (!billingAddress) {
            alert('Please enter your billing address');
            document.getElementById('billingAddress').focus();
            return false;
        }

        if (!billingCity) {
            alert('Please enter your city');
            document.getElementById('billingCity').focus();
            return false;
        }

        if (!billingZip) {
            alert('Please enter your ZIP code');
            document.getElementById('billingZip').focus();
            return false;
        }

        return true;
    }

    /**
     * Form Submission
     */
    const form = document.getElementById('cardPaymentForm');
    const payButton = document.getElementById('payButton');

    if (form && payButton) {
        form.addEventListener('submit', function(e) {
            if (!validateForm()) {
                e.preventDefault();
                return;
            }

            payButton.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Processing Payment...';
            payButton.disabled = true;
            payButton.classList.add('opacity-75', 'cursor-not-allowed');
        });
    }

    // Initialize
    initializeCard();
    console.log('💳 Card system ready | Max name length: ' + MAX_CARDHOLDER_NAME_LENGTH);
});