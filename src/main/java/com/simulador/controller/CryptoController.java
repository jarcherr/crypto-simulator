package com.simulador.controller;

import com.simulador.model.Criptomoneda;
import com.simulador.model.Transaccion;
import com.simulador.model.Usuario;
import com.simulador.repository.CriptoRepository;
import com.simulador.repository.TransaccionRepository;
import com.simulador.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Controller
public class CryptoController {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private CriptoRepository criptoRepo;

    @Autowired
    private TransaccionRepository transaccionRepo;

    // Inicializar criptomonedas y un usuario de prueba al arrancar
    @PostConstruct
    public void init() {
        if (criptoRepo.count() == 0) {
            Criptomoneda btc = new Criptomoneda();
            btc.setNombre("Bitcoin"); btc.setSimbolo("BTC"); btc.setPrecioActual(65000.0);
            criptoRepo.save(btc);

            Criptomoneda eth = new Criptomoneda();
            eth.setNombre("Ethereum"); eth.setSimbolo("ETH"); eth.setPrecioActual(3500.0);
            criptoRepo.save(eth);

            Criptomoneda doge = new Criptomoneda();
            doge.setNombre("Dogecoin"); doge.setSimbolo("DOGE"); doge.setPrecioActual(0.15);
            criptoRepo.save(doge);
        }

        if (usuarioRepo.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNombre("Administrador");
            admin.setEmail("admin@admin.com");
            admin.setPassword("1234");
            admin.setAdmin(true);
            usuarioRepo.save(admin);

            Usuario demo = new Usuario();
            demo.setNombre("Estudiante Demo");
            demo.setEmail("demo@demo.com");
            demo.setPassword("1234");
            demo.setSaldoFicticio(10000.0); // Saldo inicial de $10,000
            usuarioRepo.save(demo);
        }
    }

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, Model model) {
        Usuario u = usuarioRepo.findByEmail(email);
        if (u != null && u.getPassword().equals(password)) {
            if (!u.isActivo()) {
                model.addAttribute("error", "Su cuenta ha sido suspendida.");
                return "login";
            }
            if (u.isAdmin()) {
                return "redirect:/admin";
            }
            return "redirect:/dashboard?userId=" + u.getId();
        }
        model.addAttribute("error", "Credenciales inválidas.");
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam Long userId, Model model) {
        Usuario u = usuarioRepo.findById(userId).orElse(null);
        List<Criptomoneda> criptos = criptoRepo.findAll();
        List<Transaccion> transacciones = transaccionRepo.findByUsuarioId(userId);

        model.addAttribute("usuario", u);
        model.addAttribute("criptomonedas", criptos);
        model.addAttribute("transacciones", transacciones);
        return "dashboard";
    }

    @PostMapping("/operar")
    public String operar(@RequestParam Long userId, 
                         @RequestParam Long criptoId, 
                         @RequestParam String tipo, 
                         @RequestParam double montoUSD,
                         org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        
        Usuario u = usuarioRepo.findById(userId).orElse(null);
        Criptomoneda c = criptoRepo.findById(criptoId).orElse(null);

        if (u != null && c != null && montoUSD > 0) {
            double cantidadCripto = montoUSD / c.getPrecioActual();

            // Validación 1: Volumen mínimo de 0.01 unidades
            if (cantidadCripto < 0.01) {
                redirectAttributes.addFlashAttribute("error", "⚠️ El monto es muy bajo. Debes comprar al menos 0.01 unidades de " + c.getSimbolo() + ".");
                return "redirect:/dashboard?userId=" + userId;
            }

            if (tipo.equals("COMPRA")) {
                // Validación 2: Saldo suficiente
                if (u.getSaldoFicticio() >= montoUSD) {
                    u.setSaldoFicticio(u.getSaldoFicticio() - montoUSD);
                    usuarioRepo.save(u);

                    Transaccion t = new Transaccion();
                    t.setTipo("COMPRA");
                    t.setCantidad(cantidadCripto);
                    t.setPrecioUnitario(c.getPrecioActual());
                    t.setUsuario(u);
                    t.setCriptomoneda(c);
                    transaccionRepo.save(t);
                    
                    redirectAttributes.addFlashAttribute("exito", "¡Compra realizada con éxito!");
                } else {
                    redirectAttributes.addFlashAttribute("error", "❌ Saldo insuficiente en USD para completar la compra.");
                }
            } else if (tipo.equals("VENTA")) {
                u.setSaldoFicticio(u.getSaldoFicticio() + montoUSD);
                usuarioRepo.save(u);

                Transaccion t = new Transaccion();
                t.setTipo("VENTA");
                t.setCantidad(cantidadCripto);
                t.setPrecioUnitario(c.getPrecioActual());
                t.setUsuario(u);
                t.setCriptomoneda(c);
                transaccionRepo.save(t);
                
                redirectAttributes.addFlashAttribute("exito", "¡Venta realizada con éxito!");
            }
        }
        return "redirect:/dashboard?userId=" + userId;
    }
    
    /*
    @PostMapping("/operar")
    public String operar(@RequestParam Long userId, @RequestParam Long criptoId, @RequestParam String tipo, @RequestParam double montoUSD) {
        Usuario u = usuarioRepo.findById(userId).orElse(null);
        Criptomoneda c = criptoRepo.findById(criptoId).orElse(null);

        if (u != null && c != null && montoUSD > 0) {
            // Calcular cuántas fracciones de cripto equivalen esos USD
            double cantidadCripto = montoUSD / c.getPrecioActual();

            // REGLA: Validar que la cantidad comprada/vendida sea al menos 0.01 unidades de la cripto
            if (cantidadCripto < 0.01) {
                // Si es menor a 0.01 (como intentar comprar 1 USD de Bitcoin), no procede
                return "redirect:/dashboard?userId=" + userId;
            }

            if (tipo.equals("COMPRA")) {
                if (u.getSaldoFicticio() >= montoUSD) {
                    u.setSaldoFicticio(u.getSaldoFicticio() - montoUSD);
                    usuarioRepo.save(u);

                    Transaccion t = new Transaccion();
                    t.setTipo("COMPRA");
                    t.setCantidad(cantidadCripto);
                    t.setPrecioUnitario(c.getPrecioActual());
                    t.setUsuario(u);
                    t.setCriptomoneda(c);
                    transaccionRepo.save(t);
                }
            } else if (tipo.equals("VENTA")) {
                u.setSaldoFicticio(u.getSaldoFicticio() + montoUSD);
                usuarioRepo.save(u);

                Transaccion t = new Transaccion();
                t.setTipo("VENTA");
                t.setCantidad(cantidadCripto);
                t.setPrecioUnitario(c.getPrecioActual());
                t.setUsuario(u);
                t.setCriptomoneda(c);
                transaccionRepo.save(t);
            }
        }
        return "redirect:/dashboard?userId=" + userId;
    }
    */
    /*
    @PostMapping("/operar")
    public String operar(@RequestParam Long userId, @RequestParam Long criptoId, @RequestParam String tipo, @RequestParam double cantidad) {
        Usuario u = usuarioRepo.findById(userId).orElse(null);
        Criptomoneda c = criptoRepo.findById(criptoId).orElse(null);

        if (u != null && c != null) {
            double costoTotal = cantidad * c.getPrecioActual();
            if (tipo.equals("COMPRA")) {
                if (u.getSaldoFicticio() >= costoTotal) {
                    u.setSaldoFicticio(u.getSaldoFicticio() - costoTotal);
                    usuarioRepo.save(u);

                    Transaccion t = new Transaccion();
                    t.setTipo("COMPRA");
                    t.setCantidad(cantidad);
                    t.setPrecioUnitario(c.getPrecioActual());
                    t.setUsuario(u);
                    t.setCriptomoneda(c);
                    transaccionRepo.save(t);
                }
            } else if (tipo.equals("VENTA")) {
                // Lógica simplificada de venta sumando al saldo
                u.setSaldoFicticio(u.getSaldoFicticio() + costoTotal);
                usuarioRepo.save(u);

                Transaccion t = new Transaccion();
                t.setTipo("VENTA");
                t.setCantidad(cantidad);
                t.setPrecioUnitario(c.getPrecioActual());
                t.setUsuario(u);
                t.setCriptomoneda(c);
                transaccionRepo.save(t);
            }
        }
        return "redirect:/dashboard?userId=" + userId;
    }
    */
    
    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("usuarios", usuarioRepo.findAll());
        model.addAttribute("transacciones", transaccionRepo.findAll());
        return "admin";
    }

    @PostMapping("/admin/suspender")
    public String suspender(@RequestParam Long userId) {
        Usuario u = usuarioRepo.findById(userId).orElse(null);
        if (u != null) {
            u.setActivo(!u.isActivo()); // Cambia estado para dar de baja / suspender
            usuarioRepo.save(u);
        }
        return "redirect:/admin";
    }
    
    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/registrar")
    public String registrarUsuario(@RequestParam String nombre, @RequestParam String email, @RequestParam String password, Model model) {
        Usuario existente = usuarioRepo.findByEmail(email);
        if (existente != null) {
            model.addAttribute("error", "El correo ya está registrado.");
            return "registro";
        }

        Usuario nuevo = new Usuario();
        nuevo.setNombre(nombre);
        nuevo.setEmail(email);
        nuevo.setPassword(password);
        nuevo.setSaldoFicticio(10000.0); // Asigna los $10,000 iniciales exigidos
        usuarioRepo.save(nuevo);

        return "redirect:/?registrado=true";
    }
    
}