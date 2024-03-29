package com.hms.controller;

import com.hms.helpers.Constant;
import com.hms.model.*;
import com.hms.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Controller
@SessionAttributes("roles")
public class AdminController {

    private final UserService userService;
    private final RoomService roomService;
    private final RoomTypeService roomTypeService;
    private final BookingService bookingService;
    private final UserProfileService userProfileService;
    private final MessageSource messageSource;

    @Autowired
    public AdminController(@Lazy UserService userService, RoomService roomService, RoomTypeService roomTypeService, BookingService bookingService, UserProfileService userProfileService, MessageSource messageSource) {
        this.userService = userService;
        this.roomService = roomService;
        this.roomTypeService = roomTypeService;
        this.bookingService = bookingService;
        this.userProfileService = userProfileService;
        this.messageSource = messageSource;
    }

    /**
     * Set additional attributes required for all Admin pages
     *
     * @param model Response model for Admin page
     */
    private void setAttributes(ModelMap model) {
        List<User> customers = userService.findAllCustomers();
        List<User> admins = userService.findAllAdmins();
        List<Room> rooms = roomService.findAllRooms();
        List<Booking> bookings = bookingService.findAllBookings();
        model.addAttribute("username", getPrincipal());
        model.addAttribute("customers", customers);
        model.addAttribute("admins", admins);
        model.addAttribute("rooms", rooms);
        model.addAttribute("bookings", bookings);
        model.addAttribute("totalcustomers", customers.size());
        model.addAttribute("totaladmins", admins.size());
        model.addAttribute("totalbookings", bookings.size());
        model.addAttribute("totalrooms", rooms.size());
    }

    @RequestMapping(value = "/admin")
    public String adminHome(ModelMap model) {
        setAttributes(model);
        return "admin";
    }


    /**
     * This method will provide the medium to add a new user.
     */
    @RequestMapping(value = "/admin/new/user", method = RequestMethod.GET)
    public String newStaff(ModelMap model) {
        User user = new User();
        model.addAttribute("user", user);
        model.addAttribute("edit", false);
        model.addAttribute("loggedinuser", getPrincipal());
        return "adminCreateUser";
    }

    /**
     * This method will be called on form submission, handling POST request for
     * saving user in database. It also validates the user input
     */
    @RequestMapping(value = "/admin/new/user", method = RequestMethod.POST)
    public String saveStaff(@ModelAttribute User user, BindingResult result,
                            ModelMap model, RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            return "adminCreateUser";
        }

        if (!userService.isUserUsernameUnique(user.getId(), user.getUsername())) {
            FieldError usernameError = new FieldError("user", "username", messageSource.getMessage("non.unique.username", new String[]{user.getUsername()}, Locale.getDefault()));
            result.addError(usernameError);
            return "adminCreateUser";
        } else if (!userService.isUserEmailUnique(user.getId(), user.getEmail())) {
            FieldError emailError = new FieldError("user", "email", messageSource.getMessage("non.unique.email", new String[]{user.getEmail()}, Locale.getDefault()));
            result.addError(emailError);
            return "adminCreateUser";
        }

        userService.saveUser(user);

        redirectAttrs.addFlashAttribute("success", "User " + user.getFirstName() + " " + user.getLastName() + " was created successfully");
        return "redirect:/admin";
    }

    /**
     * This method will provide the medium to update an existing user.
     */
    @RequestMapping(value = {"admin/user/edit-{username}"}, method = RequestMethod.GET)
    public String editUser(@PathVariable String username, ModelMap model, RedirectAttributes redirectAttrs) {

        User user = userService.findByUsername(username);
        if (user == null) {
            redirectAttrs.addFlashAttribute("success", "Request user: " + username + " does not exist in database.");
            return "redirect:/manage";
        }
        user.setPassword("");

        model.addAttribute("user", user);
        model.addAttribute("edit", true);
        model.addAttribute("loggedinuser", getPrincipal());
        return "editUser";
    }

    /**
     * This method will be called on form submission, handling POST request for
     * updating user in database. It also validates the user input
     *
     * @link /admin/user/edit/delete-myname
     */
    @RequestMapping(value = {"admin/user/edit-{username}"}, method = RequestMethod.POST)
    public String updateUser(@ModelAttribute User user, BindingResult result,
                             @PathVariable String username, RedirectAttributes redirectAttrs) {

        if (result.hasErrors()) {
            return "editUser";
        }

        if (!userService.isUserUsernameUnique(user.getId(), user.getUsername())) {
            FieldError usernameError = new FieldError("user", "username", messageSource.getMessage("non.unique.username", new String[]{user.getUsername()}, Locale.getDefault()));
            result.addError(usernameError);
            return "editUser";
        } else if (!userService.isUserEmailUnique(user.getId(), user.getEmail())) {
            FieldError emailError = new FieldError("user", "email", messageSource.getMessage("non.unique.email", new String[]{user.getEmail()}, Locale.getDefault()));
            result.addError(emailError);
            return "editUser";
        }
        User u = userService.findByUsername(username);
        u.setFirstName(user.getFirstName());
        u.setLastName(user.getLastName());
        u.setUserProfiles(user.getUserProfiles());
        u.setUsername(user.getUsername());
        u.setEmail(user.getEmail());
        userService.updateUser(u);
        redirectAttrs.addFlashAttribute("success", "User: " + username + " was updated successfully");
        return "redirect:/admin";
    }

    /**
     * This method will delete user by it's Username
     *
     * @link /admin/user/delete-myname
     */
    @RequestMapping(value = {"admin/user/delete-{username}"}, method = RequestMethod.GET)
    public String deleteUser(@PathVariable String username, RedirectAttributes redirectAttrs) {

        if (userService.findByUsername(username) == null) {
            redirectAttrs.addFlashAttribute("success", "Requested user: " + username + " does not exist in database.");
            return "redirect:/admin";
        }

        userService.deleteUserByUsername(username);
        redirectAttrs.addFlashAttribute("success", "User " + username + " was deleted successfully");
        return "redirect:/admin";
    }

    /**
     * This method will provide user roles to all views
     */
    @ModelAttribute("roles")
    public List<UserProfile> initializeProfiles() {
        return userProfileService.findAll();
    }


    /**
     * This method will provide the medium to add a new room.
     */
    @RequestMapping(value = "/admin/new/room", method = RequestMethod.GET)
    public String newRoom(ModelMap model) {
        Room room = new Room();
        model.addAttribute("room", room);
        model.addAttribute("edit", false);
        return "adminAddRoom";
    }


    /**
     * This method will be called on form submission, handling POST request for
     * saving room in database.
     */
    @RequestMapping(value = "/admin/new/room", method = RequestMethod.POST)
    public String saveRoom(@ModelAttribute Room room, BindingResult result, RedirectAttributes redirectAttrs,
                           MultipartHttpServletRequest request) throws IOException {

        if (result.hasErrors()) {
            return "adminAddRoom";
        }

        if (!roomService.isRoomNameUnique(room.getId(), room.getName())) {
            FieldError nameError = new FieldError("room", "name", messageSource.getMessage("non.unique.room", new String[]{room.getName()}, Locale.getDefault()));
            result.addError(nameError);
            return "adminAddRoom";
        }

        room.setStatus(Constant.ROOM_STATUS.VERIFIED);
        roomService.saveRoom(room);
        redirectAttrs.addFlashAttribute("success", "Room " + room.getName() + " was added successfully");
        return "redirect:/admin";
    }

    /**
     * @param name - name of room
     * @link /admin/new/room/check?name=roomName
     */
    @RequestMapping(value = "/admin/room/check")
    public
    @ResponseBody
    String checkRoomAvailability(@RequestParam("name") String name) {
        if (roomService.isRoomNameUnique(null, name)) return "Available";
        return "Not Available";
    }

    @RequestMapping(value = "/admin/room/edit-{id}", method = RequestMethod.GET)
    public String editRoom(@PathVariable Integer id, ModelMap model) {
        Room room = roomService.findById(id);
        if (room == null)
            return "redirect:/admin";
        model.addAttribute("room", room);
        model.addAttribute("edit", true);
        return "editRoom";
    }

    /**
     * Request handler for saving the updated
     *
     * @link /admin/room/edit-xxxx
     */
    @RequestMapping(value = "/admin/room/edit-{id}", method = RequestMethod.POST)
    public String updateRoom(@ModelAttribute Room room, BindingResult result,
                             @PathVariable Integer id,
                             MultipartHttpServletRequest request, RedirectAttributes redirectAttrs) throws IOException {

        if (result.hasErrors()) {
            return "editRoom";
        }

        if (!roomService.isRoomNameUnique(room.getId(), room.getName())) {
            FieldError nameError = new FieldError("room", "name", messageSource.getMessage("non.unique.room", new String[]{room.getName()}, Locale.getDefault()));
            result.addError(nameError);
            return "editRoom";
        } /*else if (room.getPrice() < room.getType().getBasePrice()) {
            FieldError priceError = new FieldError("room", "price", messageSource.getMessage("conflict.room_price", new String[]{room.getType().getType()}, Locale.getDefault()));
            result.addError(priceError);
            return "editRoom";
        }*/

        Room r = roomService.findById(id);
/*        if (!(r.getPrice().equals(room.getPrice()))) {
            r.setStatus(Constant.ROOM_STATUS.UNVERIFIED);
            r.setPrice(room.getPrice());
        }*/
        r.setType(room.getType());
/*
        r.setBath(room.getBath());
*/
        r.setBooking(room.getBooking());
        /*r.setBed(room.getBed());*/
/*
        r.setCapacity(room.getCapacity());
*/
        r.setDescription(room.getDescription());
        r.setName(room.getName());
        roomService.updateRoom(r);
        redirectAttrs.addFlashAttribute("success", "Room " + room.getName() + " was updated successfully");
        return "redirect:/admin";
    }


    /**
     * Deletes the room and redirects url with appropriate message
     *
     * @return redirect url
     * @link /admin/room/delete-xxxx
     */
    @RequestMapping(value = "/admin/room/delete-{id}", method = RequestMethod.GET)
    public String deleteRoom(@PathVariable Integer id, RedirectAttributes redirectAttrs) {

        if (roomService.findById(id) == null) {
            redirectAttrs.addFlashAttribute("success", "Room with Id " + id + "does not exit.");
            return "redirect:/admin";
        }
        roomService.deleteRoomById(id);
        redirectAttrs.addFlashAttribute("success", "Room No " + id + " was removed successfully.");
        return "redirect:/admin";
    }

    /**
     * This method will provide RoomType list to views
     */
    @ModelAttribute("types")
    public List<RoomType> initializeTypes() {
        return roomTypeService.findAll();
    }

    /**
     * This method returns the principal[user-name] of logged-in user.
     */
    private String getPrincipal() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return CustomerController.getCurrentUserName(principal);
    }
}