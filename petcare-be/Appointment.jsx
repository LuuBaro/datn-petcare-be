const handleApiBooking = async () => {
    if (!selectedTime || !customerInfo.fullName || !customerInfo.phone || !customerInfo.paymentType) {
      showToast("Vui lòng điền đầy đủ thông tin!", "warning");
      return;
    }
  
    if (!selectedDate || !selectedTime || !pets || pets.length === 0 || !selectedSlots || selectedSlots.length === 0) {
      showToast("Vui lòng chọn ngày, thời gian và thông tin thú cưng!", "warning");
      return;
    }
  
    const isPetInfoValid = pets.every((pet) => pet.petType && pet.service && pet.weight && pet.price);
    if (!isPetInfoValid) {
      showToast("Vui lòng nhập đầy đủ thông tin cho tất cả thú cưng!", "warning");
      return;
    }
  
    try {
      setIsLoading(true);
  
      const formatTime = (timeStr) => {
        const [hours, minutes] = timeStr.split(':').map(Number);
        return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:00`;
      };
  
      const totalAmount = pets.reduce((sum, pet) => sum + (pet.price || 0), 0);
      const depositAmount = calculateDeposit();
      const paidAmount = customerInfo.paymentType === 'full' ? totalAmount : depositAmount;
  
      // Log dữ liệu pet ban đầu để debug
      console.log("Pet data before formatting:", JSON.stringify(pets, null, 2));
      
      // Kiểm tra và chuyển đổi dữ liệu pet một cách cẩn thận
      const formattedPets = pets.map((pet) => {
        const petServiceId = parseInt(pet.service, 10);
        const petWeightId = parseInt(pet.weight, 10);
        
        // Kiểm tra dữ liệu trước khi gửi
        if (isNaN(petServiceId) || !petServiceId) {
          throw new Error(`Dịch vụ không hợp lệ cho thú cưng ${pet.name || "Không tên"}`);
        }
        
        if (isNaN(petWeightId) || !petWeightId) {
          throw new Error(`Cân nặng không hợp lệ cho thú cưng ${pet.name || "Không tên"}`);
        }

        return {
          name: pet.name || `Thú cưng ${pets.indexOf(pet) + 1}`,
          petType: pet.petType.toUpperCase(),
          petServiceId: petServiceId,
          petWeightId: petWeightId,
          service: pet.service, // Giữ lại giá trị gốc cho tương thích
          weight: pet.weight, // Giữ lại giá trị gốc cho tương thích
          note: pet.note || "",
          price: pet.price || 0,
        };
      });
      
      // Log dữ liệu sau khi đã định dạng để kiểm tra
      console.log("Formatted pet data:", JSON.stringify(formattedPets, null, 2));

      const payload = {
        date: selectedDate.toISOString().split("T")[0],
        time: formatTime(selectedTime),
        customerName: customerInfo.fullName,
        phone: customerInfo.phone,
        paymentType: customerInfo.paymentType,
        depositAmount: depositAmount,
        totalAmount: totalAmount,
        paidAmount: paidAmount,
        appointmentSlots: selectedSlots,
        pets: formattedPets
      };
  
      console.log("Sending payload to checkout:", JSON.stringify(payload, null, 2));
  
      const isAvailable = await BookingService.checkSlotAvailability(
        payload.date,
        selectedTime,
        payload.pets.length
      );
  
      if (!isAvailable) {
        showToast("Khung giờ này đã hết slot trống. Vui lòng chọn khung giờ khác.", "error");
        await fetchBookingStatusAndSlots();
        setIsLoading(false);
        return;
      }
  
      setIsLoading(false);
      navigate(`/checkout-payment?date=${payload.date}&source=booking`, {
        state: {
          bookingData: payload,
        },
      });
    } catch (error) {
      console.error("Error processing booking:", error);
      showToast(`Đặt lịch thất bại: ${error.message || "Lỗi hệ thống"}`, "error");
      fetchBookingStatusAndSlots();
      setIsLoading(false);
    }
  }; 