# Test cases - Module Lịch tập (Tuần 4)

Kết quả chạy: `mvn -q -Dtest='com.gym.scheduling.**' test`
Kết quả chạy cuối: 44 ca chạy, 44 đạt, 0 lỗi, 0 bỏ qua. Ca đặt đồng thời chạy trên SQL Server thật.

| ID | Ca kiểm thử | Kết quả mong đợi | Bằng chứng tự động |
|---|---|---|---|
| SCH-01 | Tuần không có khoảng bận | Trả về toàn bộ khoảng giờ có thể đặt | `FreeSlotFinderTest.noBusyIntervalsReturnsEntireWindow` |
| SCH-02 | Toàn bộ giờ mở cửa đã bận | Không trả về giờ trống | `FreeSlotFinderTest.fullyBusyWindowReturnsNoFreeSlots` |
| SCH-03 | Hai khoảng bận chồng lấn | Gộp thành một khoảng bận liên tục | `FreeSlotFinderTest.overlappingIntervalsAreMerged` |
| SCH-04 | Khoảng bận nằm trong khoảng bận khác | Gộp đúng, không sinh giờ trống giả | `FreeSlotFinderTest.nestedIntervalsAreMerged` |
| SCH-05 | Hai khoảng bận tiếp giáp | Gộp thành một khoảng bận | `FreeSlotFinderTest.adjacentBusyIntervalsAreMerged` |
| SCH-06 | Danh sách khoảng bận không theo thứ tự | Kết quả giờ trống vẫn chính xác | `FreeSlotFinderTest.unsortedIntervalsStillProduceCorrectResult` |
| SCH-07 | Khoảng bận bắt đầu trước đầu cửa sổ | Cắt về đầu cửa sổ trước khi tính | `FreeSlotFinderTest.intervalCrossingWindowStartIsClipped` |
| SCH-08 | Khoảng bận kết thúc sau cuối cửa sổ | Cắt về cuối cửa sổ trước khi tính | `FreeSlotFinderTest.intervalCrossingWindowEndIsClipped` |
| SCH-09 | Khoảng bận nằm ngoài cửa sổ | Bỏ qua khoảng không giao cửa sổ | `FreeSlotFinderTest.intervalsOutsideWindowAreIgnored` |
| SCH-10 | Khoảng bận chạm đầu cửa sổ | Không tạo khoảng trống trước giờ mở cửa | `FreeSlotFinderTest.busyIntervalAtBeginning` |
| SCH-11 | Khoảng bận chạm cuối cửa sổ | Không tạo khoảng trống sau giờ đóng cửa | `FreeSlotFinderTest.busyIntervalAtEnd` |
| SCH-12 | Khoảng bận ở giữa cửa sổ | Trả về hai khoảng trống hai bên | `FreeSlotFinderTest.busyIntervalInMiddleReturnsTwoFreeSlots` |
| SCH-13 | Cửa sổ có đầu và cuối bằng nhau | Từ chối khoảng thời gian không hợp lệ | `FreeSlotFinderTest.equalWindowBoundariesAreRejected` |
| SCH-14 | Khoảng bận có đầu sau cuối | Từ chối dữ liệu khoảng thời gian sai | `FreeSlotFinderTest.reversedIntervalIsRejected` |
| SCH-15 | Khoảng bận phủ kín toàn cửa sổ | Không sinh khoảng trống | `FreeSlotFinderTest.intervalsCoveringWholeWindowLeaveNoFreeTime` |
| SCH-16 | Trainer xem giờ trống trong ngày chưa có lịch | Trả về giờ bắt đầu theo bước 15 phút cho buổi một giờ | `SchedulingServicesTest.availabilitySupportsTrainerAndReturnsFreeStarts` |
| SCH-17 | Người chưa đăng nhập yêu cầu giờ trống | Từ chối bằng lỗi phân quyền | `SchedulingServicesTest.availabilityRejectsAnonymousUsers` |
| SCH-18 | Trainer tạo lớp, sau đó Manager gửi dữ liệu sai | Trainer bị chặn; Manager đi tiếp đến kiểm tra dữ liệu | `SchedulingServicesTest.classSchedulingRequiresManagerOrReceptionist` |
| SCH-19 | Trainer đặt lịch PT | Từ chối bằng lỗi phân quyền | `SchedulingServicesTest.ptBookingRequiresManagerOrReceptionist` |
| SCH-20 | Trainer xem danh sách lớp và thử đăng ký | Cho xem; không cho đăng ký | `SchedulingServicesTest.classRegistrationAllowsTrainerToViewButNotRegister` |
| SCH-21 | Dời PT vào lịch đang bị chiếm | Báo xung đột và không lưu thay đổi | `SchedulingWeek3Test.reschedulingChecksConflictsAndDoesNotSaveWhenBlocked` |
| SCH-22 | Khôi phục PT đã hủy khi thiết bị bị xung đột | Từ chối khôi phục, không cập nhật trạng thái | `SchedulingWeek3Test.restoresCancelledPtOnlyAfterRoomTrainerAndEquipmentChecks` |
| SCH-23 | Khôi phục PT đã hủy khi tài nguyên còn trống | Chuyển trạng thái về `SCHEDULED` | `SchedulingWeek3Test.restoresCancelledPtWhenScheduleAndEquipmentRemainAvailable` |
| SCH-24 | Hai người đồng thời nhận chỗ cuối của lớp | Việc nhận chỗ được chuyển cho thao tác DAO nguyên tử | `SchedulingWeek3Test.classRegistrationDelegatesSeatClaimToAtomicDaoOperation` |
| SCH-25 | Gói hội viên đã hết hạn | Chặn đăng ký lớp, không chiếm chỗ | `SchedulingWeek4Test.expiredMembershipFromBillingModuleCannotRegisterForClass` |
| SCH-26 | Hội viên không có ảnh hồ sơ | Chặn đăng ký lớp trước khi kiểm tra gói | `SchedulingWeek4Test.memberWithoutPhotoFromMemberModuleCannotRegisterForClass` |
| SCH-27 | Lịch tuần tải 2.600 buổi | Nhận đủ kết quả với khoảng truy vấn thứ Hai đến thứ Hai; kết quả không sửa được | `SchedulingWeek4Test.weeklyScheduleKeepsLargeResultAndRequestsExactMondayToMondayRange` |
| SCH-28 | Hai luồng đặt bằng hai giao dịch DB cùng phòng, HLV và giờ | Chính xác một giao dịch thành công; giao dịch còn lại bị từ chối do xung đột/deadlock | `SchedulingConcurrencyIntegrationTest.simultaneousBookingsForSameRoomAndTrainerCannotBothSucceed` |
| SCH-29 | Hai buổi cùng phòng, thời gian giao nhau | Trả về buổi đang chặn lịch | `SessionServiceTest.findsRoomConflictAndReturnsBlockingSession` |
| SCH-30 | Hai buổi khác phòng, cùng khung giờ | Không báo xung đột phòng | `SessionServiceTest.doesNotReportRoomConflictForDifferentRooms` |
| SCH-31 | Hai buổi cùng HLV, thời gian giao nhau | Trả về buổi đang chặn lịch | `SessionServiceTest.findsTrainerConflictAndReturnsBlockingSession` |
| SCH-32 | Hai buổi khác HLV, cùng khung giờ | Không báo xung đột HLV | `SessionServiceTest.doesNotReportTrainerConflictForDifferentTrainers` |
| SCH-33 | Buổi sau bắt đầu đúng lúc buổi trước kết thúc | Cho phép hai buổi tiếp giáp | `SessionServiceTest.adjacentSessionsDoNotConflict` |
| SCH-34 | Kiểm tra xung đột khi cập nhật chính buổi hiện tại | Không coi buổi đó là xung đột với chính nó | `SessionServiceTest.ignoresSameSessionWhenCheckingUpdate` |
| SCH-35 | Buổi ứng viên bao trùm buổi hiện có | Phát hiện xung đột | `SessionServiceTest.reportsConflictWhenCandidateContainsExistingSession` |
| SCH-36 | Buổi ứng viên thiếu thời gian bắt đầu/kết thúc | Từ chối dữ liệu không hợp lệ | `SessionServiceTest.rejectsCandidateWithoutTimeRange` |
| SCH-37 | Giờ bận kết thúc ở phút không chia hết cho 15 | Gợi ý slot tiếp theo từ mốc 15 phút kế tiếp | `SchedulingServicesTest.availabilityRoundsBusyEndToNextQuarterHour` |
| SCH-38 | Thời lượng dưới 30, trên 180 hoặc không theo bước 15 phút | Từ chối thời lượng ngoài quy định | `SchedulingServicesTest.availabilityRejectsDurationOutsideAllowedRangeOrStep` |
| SCH-39 | Tạo lớp trước 06:00 | Từ chối và nêu giờ hoạt động | `SchedulingServicesTest.classCreationRejectsSessionsOutsideOpeningHours` |
| SCH-40 | Đặt PT kết thúc sau 22:00 | Từ chối và nêu giờ hoạt động | `SchedulingServicesTest.ptBookingRejectsSessionEndingAfterClosingTime` |
| SCH-41 | Giờ session qua ngày, ngoài giờ, thời lượng không hợp lệ hoặc lệch bước | Từ chối từng trường hợp; chấp nhận khung hợp lệ | `SchedulingWeek4Test.sessionTimesMustStayWithinDailyHoursAndQuarterHourGrid` |
| SCH-42 | Dời session với giờ không theo bước 15 phút | Từ chối trước khi truy vấn/ghi DB | `SessionServiceTest.reschedulingRejectsTimesOutsideQuarterHourGridBeforeDatabaseAccess` |
| SCH-43 | Dời buổi khi thiết bị không còn khả dụng theo trạng thái, phòng hoặc lịch khác | Chặn cập nhật và yêu cầu xử lý thiết bị trước | `SchedulingWeek4Test.reschedulingRejectsEquipmentThatIsNoLongerAvailable` |
| SCH-44 | Tạo lớp trùng giờ với lớp đã có | Thông báo tên lớp, mã buổi và giờ đang chiếm phòng | `SchedulingWeek4Test.classConflictMessageNamesTheBlockingSessionAndTime` |
