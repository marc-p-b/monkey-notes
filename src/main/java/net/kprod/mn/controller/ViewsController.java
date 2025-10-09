//package net.kprod.mn.controller;
//
//import net.kprod.mn.data.enums.NamedEntityVerb;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//
//import java.io.IOException;
//
//@Controller
//public class ViewsController {
//    @GetMapping("/")
//    public String viewHome() {
//        return "home";
//    }
//
//    @GetMapping("/v/transcript/{fileId}")
//    public String viewTranscript(@PathVariable("fileId") String fileId, Model model) {
//        model.addAttribute("fileId", fileId);
//        return "transcript";
//    }
//
//    @GetMapping("/v/agent/{fileId}")
//    public String viewAgent(Model model, @PathVariable String fileId) throws IOException {
//        model.addAttribute("fileId", fileId);
//        return "agent";
//    }
//
//    @GetMapping("/v/preferences")
//    public String viewPreferences() {
//        return "preferences";
//    }
//
//    @GetMapping("/v/processing")
//    public String viewProcessing() {
//        return "processing";
//    }
//
//    @GetMapping("/v/ne/{verb}")
//    public String viewNamedEntities(Model model, @PathVariable NamedEntityVerb verb) {
//        model.addAttribute("verb", verb);
//        return "named-entities";
//    }
//}
