Backend:

- API Endpoints:
- Auth: 
    1. POST /api/auth/register
    2. POST /api/auth/login
- Vehicles (Protected):
-   3. POST /api/vehicles: Add a new vehicle.
-   4. GET /api/vehicles: View a list of all available vehicles.
-   5. GET /api/vehicles/search: Search for vehicles by make, model, category, or price range.
-   6. PUT /api/vehicles/:id: Update a vehicle's details.
-   7. DELETE /api/vehicles/:id: Delete a vehicle (Admin only).
- Inventory (Protected):

-   8. POST /api/vehicles/:id/purchase: Purchase a vehicle, decreasing its quantity.
-   9. POST /api/vehicles/:id/restock: Restock a vehicle, increasing its quantity (Admin only).
