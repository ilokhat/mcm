import logging
import fiona
import pandas as pd

# Shape du réseau détaillé
SHPIN   = 'routes_bduni_alpes.shp'
# Résultat de l'appariement multi-critères
MCM_RES = 'results3pass.csv'
# Shape en sortie
SHPOUT  = './out/routes_bduni_alpes_matched.shp'

df = pd.read_csv(MCM_RES, sep=';')

with fiona.open(SHPIN, 'r') as source:
    sink_schema = source.schema
    sink_schema['properties']['id_250k'] = 'int'
    with fiona.open(SHPOUT, 'w', crs=source.crs, driver=source.driver, schema=sink_schema) as sink:
        for f in source:
            try:
                res = df.loc[ df['id'] == f['properties']['ID'] ]['id_250k']
                id250k = -1 if res.empty else int(res.values[0])
                f['properties'].update(id_250k = id250k)
                sink.write(f)

            except Exception:
                logging.exception("Error processing feature %s:", f['id'])

