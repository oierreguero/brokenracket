package eus.ehu.brokenracket.ui;

import eus.ehu.brokenracket.businessLogic.BlFacade;
import eus.ehu.brokenracket.businessLogic.BlFacadeImplementation;
import eus.ehu.brokenracket.domain.Booking;
import eus.ehu.brokenracket.domain.Court;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Calendar;
// MainUIController.java

public class MainUIController {

    @FXML
    private ComboBox<Court> courtComboBox;

    @FXML
    private DatePicker datePicker;

    @FXML
    private Button findSlotsButton; // Button reference

    @FXML
    private ListView<Booking> slotsListView;

    @FXML
    private TextField memberNameInput;

    @FXML
    private Button bookSlotButton; // Button reference

    @FXML
    private Label statusLabel;

    private BlFacade blFacade;

    // --- Simulate logged-in user --- 
    // In a real app, this would come from a login service
    private String loggedInMemberName = "ane"; // Example logged-in user
    // You could also potentially store the full Member object if needed
    // private Member loggedInMember;
    // --- End simulation ---

    private ObservableList<Court> courtList = FXCollections.observableArrayList();
    private ObservableList<Booking> bookingList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        blFacade = BlFacadeImplementation.getInstance();

        // Initialize ComboBox for courts
        courtComboBox.setItems(courtList);
        courtComboBox.setConverter(new StringConverter<Court>() {
            @Override
            public String toString(Court court) {
                return court == null ? "" : "Court #" + court.getNumber();
            }

            @Override
            public Court fromString(String string) {
                // Not used for ComboBox selection directly
                return null;
            }
        });

        // Initialize ListView for bookings
        slotsListView.setItems(bookingList);
        slotsListView.setCellFactory(param -> new ListCell<Booking>() {
            @Override
            protected void updateItem(Booking booking, boolean empty) {
                super.updateItem(booking, empty);
                if (empty || booking == null) {
                    setText(null);
                } else {
                    setText("Slot at " + booking.getHour() + ":00");
                }
            }
        });

        // Load courts from database
        loadCourts();
        
        datePicker.setValue(LocalDate.now());

        // --- Pre-fill and disable member name input --- 
        if (loggedInMemberName != null && !loggedInMemberName.isEmpty()) {
            memberNameInput.setText(loggedInMemberName);
            memberNameInput.setDisable(true); // Disable input as user is "logged in"
            statusLabel.setText("Select Court and Date to find slots for " + loggedInMemberName);
        } else {
            memberNameInput.setDisable(false); // Enable if no simulated user
            statusLabel.setText("Select Court and Date."); // Default status
        }
         // --- End pre-fill ---
    }

    private void loadCourts() {
        // Clear any existing courts
        courtList.clear();

        try {
            // Get courts from the business logic layer
            List<Court> courts = blFacade.getCourts();
            
            // If no courts returned, show error message
            if (courts == null || courts.isEmpty()) {
                statusLabel.setText("Error: No courts available in the system.");
                return;
            }
            
            // Add all courts to the observable list
            courtList.addAll(courts);
            
            // Select the first court by default
            courtComboBox.getSelectionModel().selectFirst();
            
            // Log details about loaded courts
            System.out.println("[UI] Loaded " + courtList.size() + " courts from database:");
            for (Court court : courtList) {
                System.out.println("[UI]   - Court #" + court.getNumber());
            }
        } catch (Exception e) {
            // Handle potential issues with court retrieval
            System.err.println("Error loading courts from database: " + e.getMessage());
            e.printStackTrace();
            statusLabel.setText("Error loading courts. Please try again later.");
        }
    }

    @FXML
    private void handleFindFreeSlotsAction() {
        Court selectedCourt = courtComboBox.getSelectionModel().getSelectedItem();
        LocalDate localDate = datePicker.getValue();

        if (selectedCourt == null) {
            statusLabel.setText("Please select a court.");
            return;
        }
        if (localDate == null) {
            statusLabel.setText("Please select a date.");
            return;
        }

        // Check if selected date is within one month from current date
        LocalDate currentDate = LocalDate.now();
        LocalDate oneMonthLater = currentDate.plusMonths(1);
        
        if (localDate.isAfter(oneMonthLater)) {
            statusLabel.setText("Error: Date must be within one month from today.");
            return;
        }

        Instant instant = Instant.from(localDate.atStartOfDay(ZoneId.systemDefault()));
        Date selectedDate = Date.from(instant);

    
        Calendar cal = Calendar.getInstance();
        cal.setTime(selectedDate);
      

        try {
            System.out.println("[UI] Finding free slots for Court #: " + selectedCourt.getNumber() + " on Date: " + selectedDate);
            statusLabel.setText("Finding slots...");
            List<Booking> freeSlots = blFacade.getFreeBooks(selectedCourt, selectedDate);
            System.out.println("[UI] Received " + freeSlots.size() + " free slots from facade.");
            
            
            bookingList.setAll(freeSlots);
            statusLabel.setText(freeSlots.isEmpty() ? "No free slots found." : "Found " + freeSlots.size() + " free slots.");
        } catch (Exception e) {
            statusLabel.setText("Error finding slots: " + e.getMessage());
            bookingList.clear();
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBookSelectedSlotAction() {
        Booking selectedBooking = slotsListView.getSelectionModel().getSelectedItem();
        // String memberName = memberNameInput.getText(); // No longer read from input
        String memberName = loggedInMemberName; // Use the simulated logged-in name

        if (selectedBooking == null) {
            statusLabel.setText("Please select a slot.");
            return;
        }
        // No need to check if memberName is empty as it's pre-filled
        /*
        if (memberName == null || memberName.trim().isEmpty()) {
            statusLabel.setText("Please enter member name."); // Should not happen now
            return;
        }
        */
        if (selectedBooking.getMember() != null) {
            statusLabel.setText("Error: Slot already booked.");
            handleFindFreeSlotsAction(); // Refresh list
            return;
        }

        try {
            statusLabel.setText("Booking slot for " + memberName + "...");
            blFacade.setBook(memberName.trim(), selectedBooking);
            statusLabel.setText("Slot booked successfully for " + memberName + "!");
            // memberNameInput.clear(); // No need to clear
            slotsListView.getSelectionModel().clearSelection();
            
            // Force a refresh to get updated free slots
            Court selectedCourt = courtComboBox.getSelectionModel().getSelectedItem();
            LocalDate localDate = datePicker.getValue();
            Instant instant = Instant.from(localDate.atStartOfDay(ZoneId.systemDefault()));
            Date selectedDate = Date.from(instant);
            
            System.out.println("[UI] Refreshing free slots after booking");
            List<Booking> freeSlots = blFacade.getFreeBooks(selectedCourt, selectedDate);
            System.out.println("[UI] After booking, now showing " + freeSlots.size() + " free slots");
            bookingList.setAll(freeSlots);
            
        } catch (Exception e) {
            statusLabel.setText("Error booking slot: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 