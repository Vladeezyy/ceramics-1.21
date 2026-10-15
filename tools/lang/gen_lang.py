"""Writes uk_ua, de_de, es_es and pt_br and fills the keys missing in ru_ru, from en_us.

Terms follow the vanilla translations of 1.21.1 (terracotta, bricks, bucket, furnace, armor names). Colored names are
built from a color table per language; everything else comes from the tables below. Run from the repository root:
    python3 -I tools/lang/gen_lang.py
"""
import json
import pathlib
import re

LANG = pathlib.Path(__file__).resolve().parents[2] / 'src/main/resources/assets/ceramics/lang'

COLORS = ['white', 'orange', 'magenta', 'light_blue', 'yellow', 'lime', 'pink', 'gray', 'light_gray', 'cyan', 'purple',
          'blue', 'brown', 'green', 'red', 'black']
RAINBOW = ['red', 'orange', 'yellow', 'green', 'cyan', 'blue', 'purple', 'magenta']

# color adjectives: feminine (f), neuter (n) where the language needs them
COLOR = {
    'uk_ua': {'f': ['Біла', 'Помаранчева', 'Пурпурова', 'Блакитна', 'Жовта', 'Лаймова', 'Рожева', 'Сіра', 'Світло-сіра',
                    'Бірюзова', 'Фіолетова', 'Синя', 'Коричнева', 'Зелена', 'Червона', 'Чорна']},
    'de_de': {'f': ['Weiße', 'Orange', 'Magenta', 'Hellblaue', 'Gelbe', 'Hellgrüne', 'Rosa', 'Graue', 'Hellgraue', 'Türkise',
                    'Violette', 'Blaue', 'Braune', 'Grüne', 'Rote', 'Schwarze'],
              'n': ['Weißes', 'Orange', 'Magenta', 'Hellblaues', 'Gelbes', 'Hellgrünes', 'Rosa', 'Graues', 'Hellgraues',
                    'Türkises', 'Violettes', 'Blaues', 'Braunes', 'Grünes', 'Rotes', 'Schwarzes']},
    'es_es': {'f': ['blanca', 'naranja', 'magenta', 'azul claro', 'amarilla', 'lima', 'rosa', 'gris', 'gris claro', 'cian',
                    'morada', 'azul', 'marrón', 'verde', 'roja', 'negra']},
    'pt_br': {'f': ['Branca', 'Laranja', 'Magenta', 'Azul-Clara', 'Amarela', 'Verde-Limão', 'Rosa', 'Cinza', 'Cinza-Clara',
                    'Ciano', 'Roxa', 'Azul', 'Marrom', 'Verde', 'Vermelha', 'Preta']},
}
# "Starts on <color>" for rainbow porcelain, in the order of RAINBOW
RAINBOW_START = {
    'uk_ua': ['червоного', 'помаранчевого', 'жовтого', 'зеленого', 'бірюзового', 'синього', 'фіолетового', 'пурпурового'],
    'de_de': ['Rot', 'Orange', 'Gelb', 'Grün', 'Türkis', 'Blau', 'Violett', 'Magenta'],
    'es_es': ['rojo', 'naranja', 'amarillo', 'verde', 'cian', 'azul', 'morado', 'magenta'],
    'pt_br': ['vermelho', 'laranja', 'amarelo', 'verde', 'ciano', 'azul', 'roxo', 'magenta'],
}


def colored(lang):
    """Names built from colors: porcelain blocks, colored terracotta and porcelain cisterns, rainbow tooltips"""
    out = {}
    for i, c in enumerate(COLORS):
        f = COLOR[lang]['f'][i]
        if lang == 'uk_ua':
            out[f'block.ceramics.{c}_porcelain'] = f'{f} порцеляна'
            out[f'block.ceramics.{c}_terracotta_cistern'] = f'{f} керамічна цистерна'
            out[f'block.ceramics.{c}_porcelain_cistern'] = f'{f} порцелянова цистерна'
        elif lang == 'de_de':
            out[f'block.ceramics.{c}_porcelain'] = f'{COLOR[lang]["n"][i]} Porzellan'
            out[f'block.ceramics.{c}_terracotta_cistern'] = f'{f} Keramikzisterne'
            out[f'block.ceramics.{c}_porcelain_cistern'] = f'{f} Porzellanzisterne'
        elif lang == 'es_es':
            out[f'block.ceramics.{c}_porcelain'] = f'Porcelana {f}'
            out[f'block.ceramics.{c}_terracotta_cistern'] = f'Cisterna de terracota {f}'
            out[f'block.ceramics.{c}_porcelain_cistern'] = f'Cisterna de porcelana {f}'
        elif lang == 'pt_br':
            out[f'block.ceramics.{c}_porcelain'] = f'Porcelana {f}'
            out[f'block.ceramics.{c}_terracotta_cistern'] = f'Cisterna de Terracota {f}'
            out[f'block.ceramics.{c}_porcelain_cistern'] = f'Cisterna de Porcelana {f}'
    rainbow_name = {'uk_ua': 'Райдужна порцеляна', 'de_de': 'Regenbogenporzellan', 'es_es': 'Porcelana arcoíris',
                    'pt_br': 'Porcelana Arco-Íris'}[lang]
    start = {'uk_ua': 'Починається з {}', 'de_de': 'Beginnt mit {}', 'es_es': 'Empieza por el {}', 'pt_br': 'Começa pelo {}'}[lang]
    for i, c in enumerate(RAINBOW):
        out[f'block.ceramics.{c}_rainbow_porcelain'] = rainbow_name
        out[f'block.ceramics.{c}_rainbow_porcelain.tooltip'] = start.format(RAINBOW_START[lang][i])
    return out


# brick families: (block, stairs, slab, wall) per language
BRICKS = {
    'dark':       {'uk_ua': ('Темна цегла', 'Сходи з темної цегли', 'Плита з темної цегли', 'Стіна з темної цегли'),
                   'de_de': ('Dunkle Ziegelsteine', 'Dunkle Ziegeltreppe', 'Dunkle Ziegelstufe', 'Dunkle Ziegelsteinmauer'),
                   'es_es': ('Ladrillos oscuros', 'Escaleras de ladrillos oscuros', 'Losa de ladrillos oscuros', 'Muro de ladrillos oscuros'),
                   'pt_br': ('Tijolos Escuros', 'Escadas de Tijolos Escuros', 'Laje de Tijolos Escuros', 'Muro de Tijolos Escuros')},
    'lava':       {'uk_ua': ('Лавова цегла', 'Сходи з лавової цегли', 'Плита з лавової цегли', 'Стіна з лавової цегли'),
                   'de_de': ('Lavaziegelsteine', 'Lavaziegeltreppe', 'Lavaziegelstufe', 'Lavaziegelsteinmauer'),
                   'es_es': ('Ladrillos de lava', 'Escaleras de ladrillos de lava', 'Losa de ladrillos de lava', 'Muro de ladrillos de lava'),
                   'pt_br': ('Tijolos de Lava', 'Escadas de Tijolos de Lava', 'Laje de Tijolos de Lava', 'Muro de Tijolos de Lava')},
    'dragon':     {'uk_ua': ('Драконяча цегла', 'Сходи з драконячої цегли', 'Плита з драконячої цегли', 'Стіна з драконячої цегли'),
                   'de_de': ('Drachenziegelsteine', 'Drachenziegeltreppe', 'Drachenziegelstufe', 'Drachenziegelsteinmauer'),
                   'es_es': ('Ladrillos de dragón', 'Escaleras de ladrillos de dragón', 'Losa de ladrillos de dragón', 'Muro de ladrillos de dragón'),
                   'pt_br': ('Tijolos de Dragão', 'Escadas de Tijolos de Dragão', 'Laje de Tijolos de Dragão', 'Muro de Tijolos de Dragão')},
    'porcelain':  {'uk_ua': ('Порцелянова цегла', 'Сходи з порцелянової цегли', 'Плита з порцелянової цегли', 'Стіна з порцелянової цегли'),
                   'de_de': ('Porzellanziegelsteine', 'Porzellanziegeltreppe', 'Porzellanziegelstufe', 'Porzellanziegelsteinmauer'),
                   'es_es': ('Ladrillos de porcelana', 'Escaleras de ladrillos de porcelana', 'Losa de ladrillos de porcelana', 'Muro de ladrillos de porcelana'),
                   'pt_br': ('Tijolos de Porcelana', 'Escadas de Tijolos de Porcelana', 'Laje de Tijolos de Porcelana', 'Muro de Tijolos de Porcelana')},
    'monochrome': {'uk_ua': ('Монохромна цегла', 'Сходи з монохромної цегли', 'Плита з монохромної цегли', 'Стіна з монохромної цегли'),
                   'de_de': ('Einfarbige Ziegelsteine', 'Einfarbige Ziegeltreppe', 'Einfarbige Ziegelstufe', 'Einfarbige Ziegelsteinmauer'),
                   'es_es': ('Ladrillos monocromos', 'Escaleras de ladrillos monocromos', 'Losa de ladrillos monocromos', 'Muro de ladrillos monocromos'),
                   'pt_br': ('Tijolos Monocromáticos', 'Escadas de Tijolos Monocromáticos', 'Laje de Tijolos Monocromáticos', 'Muro de Tijolos Monocromáticos')},
    'golden':     {'uk_ua': ('Золота цегла', 'Сходи з золотої цегли', 'Плита з золотої цегли', 'Стіна з золотої цегли'),
                   'de_de': ('Goldene Ziegelsteine', 'Goldene Ziegeltreppe', 'Goldene Ziegelstufe', 'Goldene Ziegelsteinmauer'),
                   'es_es': ('Ladrillos dorados', 'Escaleras de ladrillos dorados', 'Losa de ladrillos dorados', 'Muro de ladrillos dorados'),
                   'pt_br': ('Tijolos Dourados', 'Escadas de Tijolos Dourados', 'Laje de Tijolos Dourados', 'Muro de Tijolos Dourados')},
    'marine':     {'uk_ua': ('Морська цегла', 'Сходи з морської цегли', 'Плита з морської цегли', 'Стіна з морської цегли'),
                   'de_de': ('Meeresziegelsteine', 'Meeresziegeltreppe', 'Meeresziegelstufe', 'Meeresziegelsteinmauer'),
                   'es_es': ('Ladrillos marinos', 'Escaleras de ladrillos marinos', 'Losa de ladrillos marinos', 'Muro de ladrillos marinos'),
                   'pt_br': ('Tijolos Marinhos', 'Escadas de Tijolos Marinhos', 'Laje de Tijolos Marinhos', 'Muro de Tijolos Marinhos')},
    'rainbow':    {'uk_ua': ('Райдужна цегла', 'Сходи з райдужної цегли', 'Плита з райдужної цегли', 'Стіна з райдужної цегли'),
                   'de_de': ('Regenbogenziegelsteine', 'Regenbogenziegeltreppe', 'Regenbogenziegelstufe', 'Regenbogenziegelsteinmauer'),
                   'es_es': ('Ladrillos arcoíris', 'Escaleras de ladrillos arcoíris', 'Losa de ladrillos arcoíris', 'Muro de ladrillos arcoíris'),
                   'pt_br': ('Tijolos Arco-Íris', 'Escadas de Tijolos Arco-Íris', 'Laje de Tijolos Arco-Íris', 'Muro de Tijolos Arco-Íris')},
}

# remaining strings: key -> (uk, de, es, pt_br)
TABLE = {
    'creative_tab.ceramics': ('Ceramics', 'Ceramics', 'Ceramics', 'Ceramics'),
    'block.ceramics.unfired_porcelain_block': ('Невипалена порцеляна', 'Ungebranntes Porzellan', 'Porcelana sin cocer', 'Porcelana Não Queimada'),
    'block.ceramics.kiln': ('Гончарна піч', 'Brennofen', 'Horno de cerámica', 'Forno de Cerâmica'),
    'block.ceramics.terracotta_gauge': ('Покажчик рівня', 'Füllstandsanzeige', 'Indicador de nivel', 'Medidor de Nível'),
    'block.ceramics.porcelain_gauge': ('Покажчик рівня', 'Füllstandsanzeige', 'Indicador de nivel', 'Medidor de Nível'),
    'block.ceramics.gauge.tooltip': ('Встановіть на резервуар, щоб бачити рівень рідини в ньому',
                                     'Auf einen Tank setzen, um den Füllstand der Flüssigkeit zu sehen',
                                     'Colócalo en un tanque para ver el nivel del fluido que contiene',
                                     'Coloque em um tanque para ver o nível do fluido contido'),
    'block.ceramics.clay_cistern': ('Глиняна цистерна', 'Tonzisterne', 'Cisterna de arcilla', 'Cisterna de Argila'),
    'block.ceramics.unfired_cistern': ('Невипалена цистерна', 'Ungebrannte Zisterne', 'Cisterna sin cocer', 'Cisterna Não Queimada'),
    'block.ceramics.terracotta_cistern': ('Керамічна цистерна', 'Keramikzisterne', 'Cisterna de terracota', 'Cisterna de Terracota'),
    'block.ceramics.cistern.tooltip': ('Цистерни одного кольору, поставлені одна на одну, збільшують місткість',
                                       'Gleichfarbige Zisternen übereinander vergrößern das Fassungsvermögen',
                                       'Apilar cisternas del mismo color aumenta la capacidad',
                                       'Empilhar cisternas da mesma cor aumenta a capacidade'),
    'block.ceramics.clay_faucet': ('Глиняний кран', 'Tonhahn', 'Grifo de arcilla', 'Torneira de Argila'),
    'block.ceramics.unfired_faucet': ('Невипалений кран', 'Ungebrannter Hahn', 'Grifo sin cocer', 'Torneira Não Queimada'),
    'block.ceramics.terracotta_faucet': ('Керамічний кран', 'Keramikhahn', 'Grifo de terracota', 'Torneira de Terracota'),
    'block.ceramics.porcelain_faucet': ('Порцеляновий кран', 'Porzellanhahn', 'Grifo de porcelana', 'Torneira de Porcelana'),
    'block.ceramics.faucet.tooltip': ('Переливає рідину із сусіднього резервуара в блок під собою',
                                      'Gießt Flüssigkeit aus benachbarten Behältern in den Block darunter',
                                      'Vierte el fluido de los contenedores vecinos en el bloque de abajo',
                                      'Despeja o fluido dos recipientes vizinhos no bloco abaixo'),
    'block.ceramics.clay_channel': ('Глиняний жолоб', 'Tonrinne', 'Canal de arcilla', 'Canal de Argila'),
    'block.ceramics.unfired_channel': ('Невипалений жолоб', 'Ungebrannte Rinne', 'Canal sin cocer', 'Canal Não Queimado'),
    'block.ceramics.terracotta_channel': ('Керамічний жолоб', 'Keramikrinne', 'Canal de terracota', 'Canal de Terracota'),
    'block.ceramics.porcelain_channel': ('Порцеляновий жолоб', 'Porzellanrinne', 'Canal de porcelana', 'Canal de Porcelana'),
    'block.ceramics.channel.tooltip': ('Переносить рідину з крана або інших жолобів',
                                       'Leitet Flüssigkeit von einem Hahn oder anderen Rinnen weiter',
                                       'Transporta fluido desde un grifo u otros canales',
                                       'Transporta fluido de uma torneira ou de outros canais'),
    'block.ceramics.channel.side.in': ('Рідина втікає з цього боку', 'Seite fließt jetzt hinein', 'El lado ahora fluye hacia dentro', 'O lado agora flui para dentro'),
    'block.ceramics.channel.side.out': ('Рідина витікає з цього боку', 'Seite fließt jetzt hinaus', 'El lado ahora fluye hacia fuera', 'O lado agora flui para fora'),
    'block.ceramics.channel.side.none': ('Цей бік закрито', 'Seite ist jetzt geschlossen', 'El lado ahora está cerrado', 'O lado agora está fechado'),
    'block.ceramics.channel.down.out': ('Рідина стікає вниз', 'Fließt jetzt nach unten', 'Ahora fluye hacia abajo', 'Agora flui para baixo'),
    'block.ceramics.channel.down.none': ('Стікання вниз закрито', 'Fließt nicht mehr nach unten', 'Ya no fluye hacia abajo', 'Não flui mais para baixo'),
    'tooltip.ceramics.crackable': ('З часом тріскається від гарячої рідини', 'Bekommt mit der Zeit Risse durch heiße Flüssigkeit',
                                   'Se agrieta con el tiempo si contiene fluido caliente', 'Racha com o tempo ao conter fluido quente'),
    'tooltip.ceramics.heat_resistant': ('Жаростійка', 'Hitzebeständig', 'Resistente al calor', 'Resistente ao calor'),
    'tooltip.ceramics.cracked': ('Тріщини: %s / %s', 'Risse: %s / %s', 'Grietas: %s / %s', 'Rachaduras: %s / %s'),
    'item.ceramics.unfired_porcelain': ('Невипалена порцеляна', 'Ungebranntes Porzellan', 'Porcelana sin cocer', 'Porcelana Não Queimada'),
    'item.ceramics.porcelain_brick': ('Порцелянова цеглина', 'Porzellanziegel', 'Ladrillo de porcelana', 'Tijolo de Porcelana'),
    'item.ceramics.unfired_clay_bucket': ('Глиняне відро', 'Toneimer', 'Cubo de arcilla', 'Balde de Argila'),
    'item.ceramics.empty_clay_bucket': ('Відро', 'Eimer', 'Cubo', 'Balde'),
    'item.ceramics.empty_clay_bucket.tooltip': ('Тріскається, якщо набрати гарячу рідину', 'Bekommt Risse, wenn er mit heißer Flüssigkeit gefüllt wird',
                                                'Se agrieta si se llena con un fluido caliente', 'Racha se for enchido com fluido quente'),
    'item.ceramics.cracked_empty_clay_bucket': ('Тріснуте відро', 'Rissiger Eimer', 'Cubo agrietado', 'Balde Rachado'),
    'item.ceramics.cracked_empty_clay_bucket.tooltip': ('Відро розіб\'ється, коли його спорожнити', 'Der Eimer zerbricht beim Ausleeren',
                                                        'El cubo se romperá al vaciarlo', 'O balde quebrará ao ser esvaziado'),
    'item.ceramics.fluid_clay_bucket': ('Відро', 'Eimer', 'Cubo', 'Balde'),
    'item.ceramics.fluid_clay_bucket.filled': ('Відро: %s', 'Eimer (%s)', 'Cubo con %s', 'Balde de %s'),
    'item.ceramics.cracked_fluid_clay_bucket': ('Тріснуте відро', 'Rissiger Eimer', 'Cubo agrietado', 'Balde Rachado'),
    'item.ceramics.cracked_fluid_clay_bucket.filled': ('Відро: %s', 'Eimer (%s)', 'Cubo con %s', 'Balde de %s'),
    'item.ceramics.cracked_fluid_clay_bucket.tooltip': ('Відро розіб\'ється, коли його спорожнити', 'Der Eimer zerbricht beim Ausleeren',
                                                        'El cubo se romperá al vaciarlo', 'O balde quebrará ao ser esvaziado'),
    'item.ceramics.solid_clay_bucket': ('Відро', 'Eimer', 'Cubo', 'Balde'),
    'item.ceramics.solid_clay_bucket.filled': ('Відро: %s', 'Eimer (%s)', 'Cubo con %s', 'Balde de %s'),
    'item.ceramics.cracked_solid_clay_bucket': ('Тріснуте відро', 'Rissiger Eimer', 'Cubo agrietado', 'Balde Rachado'),
    'item.ceramics.cracked_solid_clay_bucket.filled': ('Відро: %s', 'Eimer (%s)', 'Cubo con %s', 'Balde de %s'),
    'item.ceramics.cracked_solid_clay_bucket.tooltip': ('Відро розіб\'ється, коли його спорожнити', 'Der Eimer zerbricht beim Ausleeren',
                                                        'El cubo se romperá al vaciarlo', 'O balde quebrará ao ser esvaziado'),
    'item.ceramics.milk_clay_bucket': ('Відро молока', 'Milcheimer', 'Cubo con leche', 'Balde de Leite'),
    'item.ceramics.cracked_milk_clay_bucket': ('Відро молока', 'Milcheimer', 'Cubo con leche', 'Balde de Leite'),
    'item.ceramics.cracked_milk_clay_bucket.tooltip': ('Відро розіб\'ється, коли його спорожнити', 'Der Eimer zerbricht beim Ausleeren',
                                                       'El cubo se romperá al vaciarlo', 'O balde quebrará ao ser esvaziado'),
    'item.ceramics.unfired_clay_plate': ('Глиняна пластина', 'Tonplatte', 'Placa de arcilla', 'Placa de Argila'),
    'item.ceramics.clay_plate': ('Керамічна пластина', 'Keramikplatte', 'Placa de terracota', 'Placa de Terracota'),
    'item.ceramics.clay_helmet': ('Керамічний шолом', 'Keramikhelm', 'Casco de terracota', 'Capacete de Terracota'),
    'item.ceramics.clay_chestplate': ('Керамічний нагрудник', 'Keramikharnisch', 'Peto de terracota', 'Peitoral de Terracota'),
    'item.ceramics.clay_leggings': ('Керамічні наголінники', 'Keramikbeinschutz', 'Grebas de terracota', 'Calças de Terracota'),
    'item.ceramics.clay_boots': ('Керамічні чоботи', 'Keramikstiefel', 'Botas de terracota', 'Botas de Terracota'),
    'jei.ceramics.clay_repair': ('Нею можна полагодити тріснуті керамічні цистерни, крани та жолоби прямо у світі.',
                                 'Repariert rissige Keramikzisternen, -hähne und -rinnen direkt in der Welt.',
                                 'Sirve para reparar en el mundo cisternas, grifos y canales de terracota agrietados.',
                                 'Pode ser usada no mundo para consertar cisternas, torneiras e canais de terracota rachados.'),
    'container.ceramics.kiln': ('Гончарна піч', 'Brennofen', 'Horno de cerámica', 'Forno de Cerâmica'),
    'gui.recipebook.toggleRecipes.ceramics.kiln': ('Доступні для випалу', 'Brennbares wird angezeigt', 'Mostrando cocibles', 'Cozíveis'),
    'gui.jei.category.ceramics.kiln': ('Гончарна піч', 'Brennofen', 'Horno de cerámica', 'Forno de Cerâmica'),
}

# keys missing in the Russian file of the original (it still had the 1.16 keys)
RU = {
    'creative_tab.ceramics': 'Ceramics',
    'block.ceramics.cistern.tooltip': 'Бочки одного цвета, поставленные друг на друга, увеличивают ёмкость',
    'block.ceramics.faucet.tooltip': 'Переливает жидкость из соседнего резервуара в блок под собой',
    'block.ceramics.channel.tooltip': 'Переносит жидкость из крана или других жёлобов',
    'tooltip.ceramics.crackable': 'Со временем трескается от горячей жидкости',
    'tooltip.ceramics.heat_resistant': 'Жаростойкая',
    'item.ceramics.empty_clay_bucket': 'Ведро',
    'item.ceramics.empty_clay_bucket.tooltip': 'Трескается при заполнении горячей жидкостью',
    'item.ceramics.cracked_empty_clay_bucket': 'Треснувшее ведро',
    'item.ceramics.cracked_empty_clay_bucket.tooltip': 'Ведро сломается при выливании',
    'item.ceramics.fluid_clay_bucket': 'Ведро',
    'item.ceramics.fluid_clay_bucket.filled': 'Ведро: %s',
    'item.ceramics.cracked_fluid_clay_bucket': 'Треснувшее ведро',
    'item.ceramics.cracked_fluid_clay_bucket.filled': 'Ведро: %s',
    'item.ceramics.cracked_fluid_clay_bucket.tooltip': 'Ведро сломается при выливании',
    'item.ceramics.solid_clay_bucket': 'Ведро',
    'item.ceramics.solid_clay_bucket.filled': 'Ведро: %s',
    'item.ceramics.cracked_solid_clay_bucket': 'Треснувшее ведро',
    'item.ceramics.cracked_solid_clay_bucket.filled': 'Ведро: %s',
    'item.ceramics.cracked_solid_clay_bucket.tooltip': 'Ведро сломается при выливании',
}


def load(name):
    """Reads a lang file, skipping the _comment keys of the original"""
    pairs = json.loads((LANG / name).read_text(encoding='utf-8'), object_pairs_hook=lambda p: p)
    return {k: v for k, v in pairs if not k.startswith('_comment')}


def write(name, data):
    (LANG / name).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def main():
    en = load('en_us.json')
    for i, lang in enumerate(['uk_ua', 'de_de', 'es_es', 'pt_br']):
        values = {k: v[i] for k, v in TABLE.items()}
        values.update(colored(lang))
        for name, langs in BRICKS.items():
            block, stairs, slab, wall = langs[lang]
            values[f'block.ceramics.{name}_bricks'] = block
            values[f'block.ceramics.{name}_bricks_stairs'] = stairs
            values[f'block.ceramics.{name}_bricks_slab'] = slab
            values[f'block.ceramics.{name}_bricks_wall'] = wall
        missing = [k for k in en if k not in values]
        if missing:
            raise SystemExit(f'{lang}: missing {missing}')
        write(f'{lang}.json', {k: values[k] for k in en})
    # Russian: keep the original translation, add the missing keys, drop keys the mod no longer uses
    ru = load('ru_ru.json')
    ru.update({k: v for k, v in RU.items() if k not in ru})
    missing = [k for k in en if k not in ru]
    if missing:
        raise SystemExit(f'ru_ru: missing {missing}')
    write('ru_ru.json', {k: ru[k] for k in en})
    print('ok')


if __name__ == '__main__':
    main()
