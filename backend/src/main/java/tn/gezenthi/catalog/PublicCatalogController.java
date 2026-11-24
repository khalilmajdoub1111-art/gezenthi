package tn.gezenthi.catalog;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.gezenthi.catalog.flight.FlightDeal;
import tn.gezenthi.catalog.flight.FlightDealDto;
import tn.gezenthi.catalog.hotel.Hotel;
import tn.gezenthi.catalog.hotel.HotelDto;
import tn.gezenthi.catalog.omra.OmraProgram;
import tn.gezenthi.catalog.omra.OmraProgramDto;
import tn.gezenthi.catalog.service.ServiceOffer;
import tn.gezenthi.catalog.service.ServiceOfferDto;
import tn.gezenthi.catalog.trip.Trip;
import tn.gezenthi.catalog.trip.TripDto;

/** Read-only catalogue for the public website: published items only, one request. */
@RestController
@RequestMapping("/api/public")
class PublicCatalogController {

    record CatalogView(
            List<HotelDto> hotels,
            List<TripDto> trips,
            List<OmraProgramDto> omra,
            List<FlightDealDto> flights,
            List<ServiceOfferDto> services) {
    }

    private final CatalogCrud<Hotel, HotelDto> hotels;
    private final CatalogCrud<Trip, TripDto> trips;
    private final CatalogCrud<OmraProgram, OmraProgramDto> omra;
    private final CatalogCrud<FlightDeal, FlightDealDto> flights;
    private final CatalogCrud<ServiceOffer, ServiceOfferDto> services;

    PublicCatalogController(CatalogCrud<Hotel, HotelDto> hotels, CatalogCrud<Trip, TripDto> trips,
                            CatalogCrud<OmraProgram, OmraProgramDto> omra, CatalogCrud<FlightDeal, FlightDealDto> flights,
                            CatalogCrud<ServiceOffer, ServiceOfferDto> services) {
        this.hotels = hotels;
        this.trips = trips;
        this.omra = omra;
        this.flights = flights;
        this.services = services;
    }

    @GetMapping("/catalog")
    CatalogView catalog() {
        return new CatalogView(hotels.findPublished(), trips.findPublished(), omra.findPublished(),
                flights.findPublished(), services.findPublished());
    }
}
