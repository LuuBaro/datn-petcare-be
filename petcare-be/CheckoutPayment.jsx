const handlePayment = async () => {
    try {
        setLoading(true);

        const totalAmount = bookingData.totalAmount || 0;
        const depositAmount = bookingData.depositAmount || 0;
        const paidAmount = bookingData.paymentType === 'full' ? totalAmount : depositAmount;

        // Kiểm tra và log dữ liệu pet ban đầu để debug
        console.log("Original pets data:", JSON.stringify(bookingData.pets, null, 2));
        
        // Kiểm tra từng thú cưng và log chi tiết nếu thiếu thông tin
        const petsWithMissingInfo = bookingData.pets.filter(pet => 
            (!pet.service && !pet.petServiceId) || (!pet.weight && !pet.petWeightId)
        );
        
        if (petsWithMissingInfo.length > 0) {
            console.error("Pets missing service or weight info:", petsWithMissingInfo);
            throw new Error("Dịch vụ hoặc cân nặng của thú cưng không được để trống");
        }

        // Định dạng dữ liệu pets theo đúng định dạng backend yêu cầu
        const formattedPets = bookingData.pets.map(pet => {
            // Ưu tiên sử dụng petServiceId nếu có, nếu không thì dùng service
            const serviceId = pet.petServiceId ? Number(pet.petServiceId) : (pet.service ? Number(pet.service) : null);
            
            // Ưu tiên sử dụng petWeightId nếu có, nếu không thì dùng weight
            const weightId = pet.petWeightId ? Number(pet.petWeightId) : (pet.weight ? Number(pet.weight) : null);
            
            // Kiểm tra sau khi chuyển đổi
            if (!serviceId || isNaN(serviceId)) {
                console.error(`Invalid service ID for pet: ${JSON.stringify(pet)}`);
                throw new Error(`Dịch vụ của thú cưng không hợp lệ: ${pet.name || "Không tên"}`);
            }
            
            if (!weightId || isNaN(weightId)) {
                console.error(`Invalid weight ID for pet: ${JSON.stringify(pet)}`);
                throw new Error(`Cân nặng của thú cưng không hợp lệ: ${pet.name || "Không tên"}`);
            }
            
            // Chỉ trả về các trường mà backend yêu cầu, đúng định dạng
            return {
                name: pet.name || `Thú cưng ${bookingData.pets.indexOf(pet) + 1}`,
                petType: pet.petType.toUpperCase(),
                petServiceId: serviceId,
                petWeightId: weightId,
                note: pet.note || "",
                price: pet.price || 0
            };
        });

        // Log formatted pets để kiểm tra
        console.log("Formatted pets:", JSON.stringify(formattedPets, null, 2));

        const payload = {
            date: bookingData.date,
            time: bookingData.time,
            customerName: bookingData.customerName,
            phone: bookingData.phone,
            paymentType: bookingData.paymentType,
            depositAmount: depositAmount,
            totalAmount: totalAmount,
            paidAmount: paidAmount,
            pets: formattedPets,
            paymentMethod: 'ONLINE',
            paymentChannel: selectedPayment.toUpperCase(),
        };

        console.log("Sending booking payload:", JSON.stringify(payload, null, 2));
        const savedAppointment = await BookingService.bookAppointment(payload);

        if (savedAppointment.success) {
            setSuccessMessage("Thanh toán thành công! Lịch hẹn đã được xác nhận.");
            setAppointmentDetails(payload);
            setShowSuccessModal(true);
        } else {
            throw new Error(savedAppointment.message || "Không thể đặt lịch.");
        }
    } catch (error) {
        console.error("Payment error:", error);
        alert(`Lỗi thanh toán: ${error.message || "Đã xảy ra lỗi"}`);
    } finally {
        setLoading(false);
    }
}; 