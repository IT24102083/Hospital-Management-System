document.addEventListener('DOMContentLoaded', () => {
    // DOM Elements
    const searchInput = document.getElementById('search-input');
    const categoryFilter = document.getElementById('category-filter');
    const medicineGrid = document.getElementById('medicine-grid');
    const cartContainer = document.getElementById('cart');
    const errorMessage = document.getElementById('error-message');
    const retryButton = document.getElementById('retry-button');
    const loadingOverlay = document.getElementById('loadingOverlay');
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const mobileMenu = document.getElementById('mobileMenu');

    // State variables
    let medicines = [];
    let filteredMedicines = [];
    let cart = [];
    let selectedCategory = 'all';
    let searchTerm = '';

    // Mobile menu toggle
    mobileMenuBtn?.addEventListener('click', function() {
        mobileMenu.classList.toggle('hidden');
    });

    // Retry loading data
    retryButton?.addEventListener('click', function() {
        errorMessage.classList.add('hidden');
        loadData();
    });

    // Search functionality
    searchInput?.addEventListener('input', function() {
        searchTerm = this.value.toLowerCase().trim();
        filterMedicines();
    });

    // Category filter dropdown
    categoryFilter?.addEventListener('change', function() {
        selectedCategory = this.value;
        updateActiveTab();
        filterMedicines();
    });

    // Initialize category tabs
    function initCategoryTabs() {
        document.querySelectorAll('.category-tab').forEach(tab => {
            tab.addEventListener('click', function(e) {
                e.preventDefault();
                selectedCategory = this.getAttribute('data-category');

                if(categoryFilter) {
                    categoryFilter.value = selectedCategory;
                }
                updateActiveTab();
                filterMedicines();
            });
        });
    }

    // Update active tab visual state
    function updateActiveTab() {
        document.querySelectorAll('.category-tab').forEach(tab => {
            const category = tab.getAttribute('data-category');
            const countSpan = tab.querySelector('.category-count');

            if(category === selectedCategory) {
                // Active state
                tab.classList.remove('text-gray-500', 'border-transparent', 'hover:text-gray-700', 'hover:border-gray-300');
                tab.classList.add('text-blue-600', 'border-blue-600');
                if (countSpan) {
                    countSpan.classList.remove('bg-gray-200', 'text-gray-900');
                    countSpan.classList.add('bg-blue-100', 'text-blue-600');
                }
            } else {
                // Inactive state
                tab.classList.remove('text-blue-600', 'border-blue-600');
                tab.classList.add('text-gray-500', 'border-transparent', 'hover:text-gray-700', 'hover:border-gray-300');
                if (countSpan) {
                    countSpan.classList.remove('bg-blue-100', 'text-blue-600');
                    countSpan.classList.add('bg-gray-200', 'text-gray-900');
                }
            }
        });
    }

    // Filter medicines based on search and category
    function filterMedicines() {
        filteredMedicines = medicines.filter(medicine => {
            const matchesSearch = searchTerm === '' ||
                (medicine.name && medicine.name.toLowerCase().includes(searchTerm)) ||
                (medicine.generic_name && medicine.generic_name.toLowerCase().includes(searchTerm)) ||
                (medicine.brand && medicine.brand.toLowerCase().includes(searchTerm)) ||
                (medicine.description && medicine.description.toLowerCase().includes(searchTerm));

            const matchesCategory = selectedCategory === 'all' || medicine.category === selectedCategory;

            return matchesSearch && matchesCategory;
        });

        renderMedicines();
    }

    // Render medicines in grid
    function renderMedicines() {
        if (!medicineGrid) return;

        if (filteredMedicines.length === 0) {
            medicineGrid.innerHTML = `
                <div class="col-span-full p-8 text-center">
                    <div class="bg-blue-50 rounded-lg p-6">
                        <i class="fas fa-info-circle text-blue-500 text-4xl mb-4"></i>
                        <h3 class="text-xl font-bold text-gray-800 mb-2">No Medicines Found</h3>
                        <p class="text-gray-600">We couldn't find any medicines matching your criteria.</p>
                    </div>
                </div>
            `;
            return;
        }

        medicineGrid.innerHTML = filteredMedicines.map(medicine => {
            const isLowStock = medicine.stock < 20;
            const isOutOfStock = medicine.stock === 0;
            let stockIndicator = '';

            if (isOutOfStock) {
                stockIndicator = `<div class="flex items-center text-red-600"><span class="text-sm font-medium">Out of Stock</span></div>`;
            } else if (isLowStock) {
                stockIndicator = `<div class="flex items-center text-orange-600"><span class="text-sm font-medium">Low Stock (${medicine.stock})</span></div>`;
            } else {
                stockIndicator = `<div class="flex items-center text-green-600"><span class="text-sm">In Stock (${medicine.stock})</span></div>`;
            }

            const imageUrl = medicine.image || `https://via.placeholder.com/300x200?text=${encodeURIComponent(medicine.name)}`;

            return `
            <div class="h-full flex flex-col hover:shadow-lg transition-all duration-300 hover:-translate-y-1 border-sky-100 rounded-lg bg-white">
                <div class="relative overflow-hidden rounded-t-lg">
                    <img src="${imageUrl}" alt="${medicine.name}" class="w-full h-32 object-cover">
                    <div class="absolute top-2 right-2 flex gap-1">
                        ${medicine.requiresPrescription ? `<span class="text-xs bg-red-100 text-red-800 px-2 py-1 rounded-full">Rx</span>` : ''}
                        <span class="text-xs bg-white/90 border-sky-200 border px-2 py-1 rounded-full">${medicine.category || 'Uncategorized'}</span>
                    </div>
                </div>
                <div class="p-3 flex-1 flex flex-col">
                    <h3 class="text-lg font-semibold text-gray-900 line-clamp-1">${medicine.name}</h3>
                    <p class="text-sm text-gray-600">${medicine.generic_name || ''}</p>
                    <p class="text-xs text-gray-500 font-medium">${medicine.brand || ''}</p>
                    <div class="flex-1 mt-3">
                        <div class="flex justify-between items-center">
                            <span class="text-sm font-medium bg-sky-50 text-sky-700 px-2 py-1 rounded border border-sky-200">${medicine.dosage || ''} ${medicine.form || ''}</span>
                            <span class="text-lg font-bold text-sky-600">$${(medicine.price || 0).toFixed(2)}</span>
                        </div>
                        <p class="text-sm text-gray-600 line-clamp-2 mt-2">${medicine.description || 'No description available.'}</p>
                        <div class="mt-2">${stockIndicator}</div>
                    </div>
                    <div class="pt-3 border-t bg-sky-50/30 -mx-3 -mb-3 mt-3 p-3">
                         <div class="flex gap-2">
                            <button data-id="${medicine.id}" class="view-details-btn flex-1 bg-white border text-sky-700 font-bold py-2 px-4 rounded text-sm">Details</button>
                            <button data-id="${medicine.id}" class="add-to-cart-btn flex-1 bg-sky-600 hover:bg-sky-700 text-white font-bold py-2 px-4 rounded text-sm" ${isOutOfStock ? 'disabled' : ''}>Add to Cart</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
        }).join('');

        // Bind view-details here
        document.querySelectorAll('.view-details-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const medicineId = this.getAttribute('data-id');
                showMedicineDetails(medicineId);
            });
        });
    }

    // Fetch medicine data from API
    async function loadData() {
        if(loadingOverlay) {
            loadingOverlay.style.display = 'flex';
            loadingOverlay.style.opacity = '1';
        }

        try {
            // In a real app, this would fetch data from your API
            const response = await fetch('/api/medicines');
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            medicines = await response.json();
            filteredMedicines = [...medicines];

            // Initialize all UI components with data from the database
            renderMedicines();
            updateCategoryUI();
            renderCart();

        } catch (error) {
            console.error('Error fetching medicine data:', error);

            // For demonstration, create sample data ONLY IF the API fails
            medicines = createSampleMedicines();
            filteredMedicines = [...medicines];

            // Initialize UI components with fallback sample data
            renderMedicines();
            updateCategoryUI();
            renderCart();

            // Optional: uncomment the line below to show an error message to the user
            // if(errorMessage) errorMessage.classList.remove('hidden');
        } finally {
            // This will run whether the fetch succeeded or failed
            if(loadingOverlay) {
                loadingOverlay.style.opacity = '0';
                setTimeout(() => {
                    loadingOverlay.style.display = 'none';
                }, 500);
            }
        }
    }

    // NEW function to update category UI (counts and dropdown)
    function updateCategoryUI() {
        const categoryCounts = medicines.reduce((counts, medicine) => {
            const category = medicine.category || 'Uncategorized';
            counts[category] = (counts[category] || 0) + 1;
            return counts;
        }, {});

        // Update counts in the static tabs
        const categoryTabs = document.querySelectorAll('.category-tab');
        if (categoryTabs.length > 0) {
            categoryTabs.forEach(tab => {
                const category = tab.getAttribute('data-category');
                const countSpan = tab.querySelector('.category-count');
                if (countSpan) {
                    const count = category === 'all' ? medicines.length : (categoryCounts[category] || 0);
                    countSpan.textContent = count;
                }
            });
        }

        // Update category filter dropdown
        if (categoryFilter) {
            const categories = ['all', ...new Set(medicines.map(m => m.category).filter(Boolean))];
            categoryFilter.innerHTML = categories.map(category => {
                const displayName = category === 'all' ? 'All Categories' : category;
                return `<option value="${category}" ${category === selectedCategory ? 'selected' : ''}>${displayName}</option>`;
            }).join('');
        }
    }


    // Show medicine details modal (no changes needed)
    function showMedicineDetails(medicineId) {
        const medicine = medicines.find(m => m.id == medicineId);
        if (!medicine) return;
        let modalContainer = document.getElementById('medicine-details-modal');
        if (!modalContainer) {
            modalContainer = document.createElement('div');
            modalContainer.id = 'medicine-details-modal';
            document.body.appendChild(modalContainer);
        }
        const sideEffects = medicine.side_effects ?
            medicine.side_effects.split(',').map(effect =>
                `<span class="text-xs bg-orange-100 text-orange-800 px-2 py-1 rounded-full">${effect.trim()}</span>`
            ).join('') :
            '<span class="text-gray-500">None listed</span>';
        const contraindications = medicine.contraindications ?
            medicine.contraindications.split(',').map(item =>
                `<span class="text-xs bg-red-100 text-red-800 px-2 py-1 rounded-full">${item.trim()}</span>`
            ).join('') :
            '<span class="text-gray-500">None listed</span>';
        modalContainer.innerHTML = `
            <div class="fixed inset-0 bg-black bg-opacity-50 z-50 flex items-center justify-center p-4">
                <div class="bg-white rounded-lg shadow-xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
                    <div class="flex justify-between items-center p-4 border-b">
                        <div>
                            <h2 class="text-2xl font-bold">${medicine.name}</h2>
                            <p class="text-gray-600">${medicine.generic_name || ''}</p>
                            <p class="text-sm text-gray-500">${medicine.brand || ''}</p>
                        </div>
                        <button class="close-modal text-gray-500 hover:text-gray-700">
                            <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>
                    <div class="p-6 grid grid-cols-1 md:grid-cols-3 gap-6">
                        <div class="space-y-4">
                            <div class="bg-gray-100 rounded-lg overflow-hidden">
                                <img src="${medicine.image || `https://via.placeholder.com/400x300?text=${encodeURIComponent(medicine.name)}`}" alt="${medicine.name}" class="w-full h-auto object-cover">
                            </div>
                            <div class="bg-blue-50 p-4 rounded-lg text-center">
                                <p class="text-3xl font-bold text-blue-600">$${(medicine.price || 0).toFixed(2)}</p>
                                <div class="flex items-center justify-center mt-2 text-green-600">
                                    <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5 mr-1" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                                    <span>${medicine.stock || 0} units available</span>
                                </div>
                                <button data-id="${medicine.id}" class="add-to-cart-modal-btn w-full mt-4 bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">Add to Cart</button>
                            </div>
                        </div>
                        <div class="md:col-span-2 space-y-6">
                            <div>
                                <h3 class="text-lg font-semibold mb-4">Medicine Details</h3>
                                <div class="grid grid-cols-2 gap-x-4 gap-y-2">
                                    <div class="flex items-center justify-between"><span class="text-gray-600">Category:</span><span class="bg-blue-100 text-blue-800 text-xs font-medium px-2.5 py-0.5 rounded">${medicine.category || 'Uncategorized'}</span></div>
                                    <div class="flex items-center justify-between"><span class="text-gray-600">Dosage:</span><span>${medicine.dosage || 'N/A'}</span></div>
                                    <div class="flex items-center justify-between"><span class="text-gray-600">Form:</span><span>${medicine.form || 'N/A'}</span></div>
                                    <div class="flex items-center justify-between"><span class="text-gray-600">Requires Prescription:</span><span>${medicine.requiresPrescription ? 'Yes' : 'No'}</span></div>
                                </div>
                            </div>
                            <div>
                                <h3 class="text-lg font-semibold mb-4">Manufacturer Info</h3>
                                <div class="grid grid-cols-2 gap-x-4 gap-y-2">
                                    <div class="flex items-center justify-between"><span class="text-gray-600">Manufacturer:</span><span>${medicine.manufacturer || 'N/A'}</span></div>
                                    <div class="flex items-center justify-between"><span class="text-gray-600">Expires:</span><span>${medicine.expiryDate || 'N/A'}</span></div>
                                </div>
                            </div>
                            <div>
                                <h3 class="text-lg font-semibold mb-2">Description</h3>
                                <p class="text-gray-700">${medicine.description || 'No description available.'}</p>
                            </div>
                            <div>
                                <h3 class="text-lg font-semibold mb-3">Side Effects</h3>
                                <div class="flex flex-wrap gap-2">${sideEffects}</div>
                            </div>
                            <div>
                                <h3 class="text-lg font-semibold mb-3">Contraindications</h3>
                                <div class="flex flex-wrap gap-2">${contraindications}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>`;
        modalContainer.querySelector('.close-modal').addEventListener('click', () => { modalContainer.innerHTML = ''; });
        modalContainer.querySelector('.add-to-cart-modal-btn').addEventListener('click', function() {
            addToCart(this.getAttribute('data-id'));
            modalContainer.innerHTML = '';
        });
    }

    // Add medicine to cart
    function addToCart(medicineId) {
        const medicine = medicines.find(m => m.id == medicineId);
        if (!medicine || medicine.stock <= 0) return;
        medicine.stock--;
        const existingItem = cart.find(item => item.medicineId == medicineId);
        if (existingItem) {
            existingItem.quantity++;
        } else {
            cart.push({ medicineId: medicine.id, name: medicine.name, price: medicine.price, image: medicine.image, quantity: 1 });
        }
        renderCart();
        renderMedicines();
        showNotification(`${medicine.name} added to cart`);
    }

    // Render cart
    function renderCart() {
        if(!cartContainer) return;
        if (cart.length === 0) {
            cartContainer.innerHTML = `
                <div class="sticky top-4 shadow-lg border-sky-200 rounded-lg bg-white">
                    <div class="p-4 bg-gradient-to-r from-sky-50 to-sky-100 rounded-t-lg"><h2 class="text-lg font-bold text-sky-800 flex items-center gap-2"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>Shopping Cart</h2></div>
                    <div class="p-6"><div class="text-center py-8"><div class="bg-sky-50 rounded-full w-16 h-16 mx-auto mb-4 flex items-center justify-center"><svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="text-sky-400"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"></path><line x1="3" y1="6" x2="21" y2="6"></line><path d="M16 10a4 4 0 0 1-8 0"></path></svg></div><p class="text-gray-600 font-medium">Your cart is empty</p><p class="text-sm text-gray-400 mt-1">Add medicines to get started</p></div></div>
                </div>`;
            return;
        }
        const subtotal = cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
        const tax = subtotal * 0.08;
        const total = subtotal + tax;
        const itemCount = cart.reduce((sum, item) => sum + item.quantity, 0);
        cartContainer.innerHTML = `
            <div class="sticky top-4 shadow-lg border-sky-200 rounded-lg bg-white">
                <div class="p-4 bg-gradient-to-r from-sky-50 to-sky-100 rounded-t-lg flex justify-between items-center">
                    <h2 class="text-lg font-bold text-sky-800 flex items-center gap-2"><svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="21" r="1"></circle><circle cx="20" cy="21" r="1"></circle><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"></path></svg>Shopping Cart</h2>
                    <span class="bg-sky-600 text-white text-xs font-semibold px-2.5 py-1 rounded-full">${itemCount} items</span>
                </div>
                <div class="p-4">
                    <div class="max-h-64 overflow-y-auto space-y-3">${cart.map(item => `<div class="flex items-center p-2 border-b"><div class="h-12 w-12 flex-shrink-0 overflow-hidden rounded bg-gray-100"><img src="${item.image || `https://via.placeholder.com/100?text=${encodeURIComponent(item.name)}`}" alt="${item.name}" class="h-full w-full object-cover"></div><div class="ml-3 flex-1"><div class="flex justify-between"><h4 class="text-sm font-medium">${item.name}</h4><span class="text-sky-600 font-bold">$${(item.price * item.quantity).toFixed(2)}</span></div><div class="flex justify-between items-center mt-1"><div class="flex items-center border rounded"><button data-id="${item.medicineId}" class="decrement-btn px-2 py-1 text-gray-600 hover:bg-gray-100">-</button><span class="px-2">${item.quantity}</span><button data-id="${item.medicineId}" class="increment-btn px-2 py-1 text-gray-600 hover:bg-gray-100">+</button></div><button data-id="${item.medicineId}" class="remove-btn text-red-500 hover:text-red-700"><svg xmlns="http://www.w3.org/2000/svg" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg></button></div></div></div>`).join('')}</div>
                    <div class="mt-4 space-y-2">
                        <div class="flex justify-between text-sm"><span class="text-gray-600">Subtotal:</span><span class="font-medium">$${subtotal.toFixed(2)}</span></div>
                        <div class="flex justify-between text-sm"><span class="text-gray-600">Tax (8%):</span><span class="font-medium">$${tax.toFixed(2)}</span></div>
                        <div class="flex justify-between text-sm"><span class="text-gray-600">Delivery:</span><span class="font-medium text-green-600">Free</span></div>
                        <div class="pt-2 border-t flex justify-between font-bold"><span>Total:</span><span class="text-sky-600">$${total.toFixed(2)}</span></div>
                    </div>
                    <button id="checkout-btn" class="w-full mt-4 bg-sky-600 hover:bg-sky-700 text-white py-2 px-4 rounded font-medium">Proceed to Checkout</button>
                </div>
            </div>`;
        cartContainer.querySelectorAll('.decrement-btn').forEach(btn => btn.addEventListener('click', () => updateCartItemQuantity(btn.dataset.id, -1)));
        cartContainer.querySelectorAll('.increment-btn').forEach(btn => btn.addEventListener('click', () => updateCartItemQuantity(btn.dataset.id, 1)));
        cartContainer.querySelectorAll('.remove-btn').forEach(btn => btn.addEventListener('click', () => removeFromCart(btn.dataset.id)));
        const checkoutBtn = cartContainer.querySelector('#checkout-btn');
        if(checkoutBtn) checkoutBtn.addEventListener('click', proceedToCheckout);
    }

    // Update cart item quantity
    function updateCartItemQuantity(medicineId, change) {
        const item = cart.find(item => item.medicineId == medicineId);
        if (!item) return;
        const medicine = medicines.find(m => m.id == medicineId);
        if (!medicine) return;
        if (change > 0 && medicine.stock <= 0) {
            showNotification('No more stock available', 'error');
            return;
        }
        item.quantity += change;
        medicine.stock -= change;
        if (item.quantity <= 0) {
            removeFromCart(medicineId);
            return;
        }
        renderCart();
        renderMedicines();
    }

    // Remove item from cart
    function removeFromCart(medicineId) {
        const itemIndex = cart.findIndex(item => item.medicineId == medicineId);
        if (itemIndex === -1) return;
        const item = cart[itemIndex];
        const medicine = medicines.find(m => m.id == medicineId);
        if (medicine) {
            medicine.stock += item.quantity;
        }
        cart.splice(itemIndex, 1);
        renderCart();
        renderMedicines();
        showNotification(`${item.name} removed from cart`, 'info');
    }

    // Proceed to checkout (placeholder)
    function proceedToCheckout(checkoutBtn) {
        if (cart.length === 0) {
            showNotification('Your cart is empty.', 'error');
            return;
        }

        checkoutBtn.disabled = true;
        checkoutBtn.innerHTML = '<i class="fas fa-spinner fa-spin mr-2"></i> Processing...';

        const cartData = cart.map(item => ({
            medicineId: item.medicineId,
            quantity: item.quantity
        }));

        fetch('/pharmacy/checkout', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(cartData)
        })
            .then(response => {
                if (!response.ok) {
                    // Check if the server sent a JSON error
                    if (response.headers.get("content-type")?.includes("application/json")) {
                        return response.json().then(err => { throw new Error(err.error || 'Checkout failed.') });
                    } else {
                        // Handle non-JSON errors (like a 401/403 HTML page if not logged in)
                        throw new Error('An error occurred. Please ensure you are logged in and try again.');
                    }
                }
                return response.json();
            })
            .then(data => {
                if (data.redirectUrl) {
                    window.location.href = data.redirectUrl;
                } else {
                    throw new Error('Invalid response from server.');
                }
            })
            .catch(error => {
                console.error('Checkout error:', error);
                showNotification(error.message, 'error');
                checkoutBtn.disabled = false;
                checkoutBtn.innerHTML = 'Proceed to Checkout';
            });
    }

    // Show notification
    function showNotification(message, type = 'success') {
        let container = document.getElementById('notification-container');
        if (!container) {
            container = document.createElement('div');
            container.id = 'notification-container';
            container.className = 'fixed bottom-4 right-4 z-[100] flex flex-col gap-2';
            document.body.appendChild(container);
        }
        const bgColor = type === 'success' ? 'bg-green-500' : type === 'error' ? 'bg-red-500' : 'bg-blue-500';
        const notification = document.createElement('div');
        notification.className = `${bgColor} text-white px-4 py-2 rounded shadow-lg flex items-center justify-between animate-fade-in-up`;
        notification.innerHTML = `<span>${message}</span><button class="ml-4 text-white hover:text-gray-200">×</button>`;
        container.appendChild(notification);
        const closeBtn = notification.querySelector('button');
        closeBtn.addEventListener('click', () => notification.remove());
        setTimeout(() => notification.remove(), 3000);
    }

    // Create sample medicines
    function createSampleMedicines() {
        return [
            { id: 1, name: "Paracetamol", generic_name: "Acetaminophen", brand: "Tylenol", category: "Pain Relief", description: "Pain reliever and fever reducer", dosage: "500mg", form: "Tablet", price: 12.50, stock: 150, image: "https://i-cf65.ch-static.com/content/dam/cf-consumer-healthcare/panadol/en_ie/ireland-products/panadol-tablets/MGK53569-GSK-Panadol-500mg-Tablets-12s-Carton-and-Blister-Front-Angle-IE-3D.png", manufacturer: "PharmaCorp", expiryDate: "31/12/2025", requiresPrescription: false, side_effects: "Nausea,Stomach pain,Loss of appetite", contraindications: "Liver disease,Alcohol dependency" },
            { id: 2, name: "Amoxicillin", generic_name: "Amoxicillin", brand: "Amoxil", category: "Antibiotics", description: "Treats bacterial infections", dosage: "250mg", form: "Capsule", price: 15.99, stock: 80, image: "https://5.imimg.com/data5/SELLER/Default/2023/1/KM/EX/LM/4373979/amoxicillin-capsule-ip.jpg", manufacturer: "MediPharm", expiryDate: "15/06/2024", requiresPrescription: true, side_effects: "Diarrhea,Stomach pain,Rash", contraindications: "Penicillin allergy" },
            { id: 3, name: "Lisinopril", generic_name: "Lisinopril", brand: "Prinivil", category: "Cardiovascular", description: "Treats high blood pressure", dosage: "10mg", form: "Tablet", price: 22.50, stock: 18, image: "https://www.medsforless.co.uk/uploads/images/products/verylarge/medsforless_co_uk_lisinopril_1614948719Lisinopril-Tablets-10mg-2.jpg", manufacturer: "HeartCare", expiryDate: "20/08/2024", requiresPrescription: true, side_effects: "Dizziness,Headache,Dry cough", contraindications: "Pregnancy,History of angioedema" },
            { id: 4, name: "Atorvastatin", generic_name: "Atorvastatin Calcium", brand: "Lipitor", category: "Cardiovascular", description: "Reduces cholesterol levels", dosage: "20mg", form: "Tablet", price: 35.75, stock: 60, image: "https://5.imimg.com/data5/SELLER/Default/2022/12/ZE/VT/PF/106495811/atorvastatin-80-mg-tablet.jpg", manufacturer: "CardioHealth", expiryDate: "10/05/2024", requiresPrescription: true, side_effects: "Muscle pain,Liver problems,Digestive issues", contraindications: "Liver disease,Pregnancy" },
            { id: 5, name: "Metformin", generic_name: "Metformin HCl", brand: "Glucophage", category: "Diabetes", description: "Treats type 2 diabetes", dosage: "500mg", form: "Tablet", price: 18.99, stock: 0, image: "https://i0.wp.com/post.healthline.com/wp-content/uploads/2021/10/metformin-1296x728-header.jpg", manufacturer: "DiabeCare", expiryDate: "25/09/2024", requiresPrescription: true, side_effects: "Nausea,Diarrhea,Vitamin B12 deficiency", contraindications: "Kidney disease,Liver disease" }
        ];
    }

    // Initialize the page
    function init() {
        loadData();
        initCategoryTabs();

        // Event delegation for add-to-cart
        document.addEventListener('click', function(e) {
            if(e.target.closest('.add-to-cart-btn')) {
                const medicineId = e.target.closest('.add-to-cart-btn').dataset.id;
                if(medicineId) {
                    addToCart(medicineId);
                }
            }
        });

        // Upload prescription button
        const uploadPrescriptionBtn = document.getElementById('upload-prescription-btn');
        if(uploadPrescriptionBtn) {
            uploadPrescriptionBtn.addEventListener('click', function() {
                showNotification('Prescription upload feature coming soon!', 'info');
            });
        }
    }

    // Start the app
    init();
});