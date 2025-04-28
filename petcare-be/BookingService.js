import axios from 'axios';

const API_URL = 'http://localhost:8080/api';

class BookingService {
    async getAvailableSlots(date) {
        try {
            const response = await axios.get(`${API_URL}/time-slots/available-slots?date=${date}`);
            return response.data;
        } catch (error) {
            console.error("Error fetching available slots:", error);
            throw error;
        }
    }

    async checkSlotAvailability(date, time, petCount) {
        try {
            const response = await axios.get(`${API_URL}/time-slots/check-availability?date=${date}&time=${time}&petCount=${petCount}`);
            return response.data;
        } catch (error) {
            console.error("Error checking slot availability:", error);
            return false;
        }
    }

    async bookAppointment(appointmentData) {
        try {
            // Kiểm tra và debug dữ liệu trước khi gửi API request
            console.log("BookingService.bookAppointment - Data được gửi đi:", JSON.stringify(appointmentData, null, 2));
            
            // Cấu trúc lại payload nếu cần để phù hợp với backend API
            const payload = {
                date: appointmentData.date,
                time: appointmentData.time,
                customerName: appointmentData.customerName,
                phone: appointmentData.phone,
                paymentType: appointmentData.paymentType,
                depositAmount: appointmentData.depositAmount,
                totalAmount: appointmentData.totalAmount,
                paidAmount: appointmentData.paidAmount,
                paymentMethod: appointmentData.paymentMethod,
                paymentChannel: appointmentData.paymentChannel,
                pets: appointmentData.pets.map(pet => ({
                    name: pet.name,
                    petType: pet.petType,
                    petServiceId: pet.petServiceId,
                    petWeightId: pet.petWeightId,
                    note: pet.note || "",
                    price: pet.price || 0
                }))
            };
            
            console.log("BookingService.bookAppointment - Payload cuối cùng:", JSON.stringify(payload, null, 2));
            
            const response = await axios.post(`${API_URL}/appointments`, payload);
            console.log("BookingService.bookAppointment - Response:", response.data);
            
            return { 
                success: response.status === 200, 
                data: response.data 
            };
        } catch (error) {
            console.error("BookingService.bookAppointment - Error:", error.response?.data || error.message);
            throw error;
        }
    }

    clearStaleBookings() {
        // Remove any stale booking data from localStorage
        localStorage.removeItem('lastBookedSlots');
        localStorage.removeItem('lastAppointmentDate');
    }
}

export default new BookingService(); 